package org.starcoin.indexer.handler;

import org.elasticsearch.client.RestHighLevelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.starcoin.api.StateRPCClient;
import org.starcoin.bean.*;
import org.starcoin.constant.StarcoinNetwork;
import org.starcoin.indexer.service.SwapPoolStatService;
import org.starcoin.indexer.service.SwapStatService;
import org.starcoin.indexer.service.SwapTxnService;
import org.starcoin.indexer.service.TokenStatService;
import com.thetransactioncompany.jsonrpc2.client.JSONRPC2SessionException;
import org.starcoin.utils.NumberUtils;
import org.starcoin.utils.StructTagUtil;
import org.starcoin.utils.SwapApiClient;
import org.starcoin.types.TypeTag;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

@Service
public class SwapHandle {

    private static final Logger logger = LoggerFactory.getLogger(SwapHandle.class);

    @Value("${starcoin.network}")
    private String network;
    private StarcoinNetwork localNetwork;
    @Value("${swap.contract.address}")
    private String contractAddress;
    @Autowired
    private StateRPCClient stateRPCClient;
    @Autowired
    private TvlService tvlService;
    @Autowired
    private SwapTxnService swapTxnService;
    @Autowired
    private TokenStatService tokenStatService;
    @Autowired
    private SwapPoolStatService swapPoolStatService;
    @Autowired
    private SwapStatService swapStatService;
    @Autowired
    private SwapApiClient swapApiClient;

    public void swapStat(long startTime, long endTime) {
        //init network
        if (localNetwork == null) {
            localNetwork = StarcoinNetwork.fromValue(network);
        }
        List<TokenStat> tokenStatList = new ArrayList<>();
        try {
            List<LiquidityPoolInfo> poolInfoList = swapApiClient.getPoolInfo(localNetwork.getValue());
            List<SwapToken> tokenList = swapApiClient.getTokens(localNetwork.getValue());
            if (tokenList == null || tokenList.isEmpty()) {
                logger.warn("get token null: {}", localNetwork.getValue());
                return;
            }

            SwapStat swapStat = new SwapStat(new Date(startTime));

            Map<String, String> tokenMapping = new HashMap<>();
            Map<String, TokenTvl> tokenTvlMapping = new HashMap<>();
            List<TokenTvl> tokenTvls = swapApiClient.getTokenTvl(localNetwork.getValue());
            for (TokenTvl tvl : tokenTvls) {
                tokenTvlMapping.put(tvl.getTokenName(), tvl);
            }
            for (SwapToken token : tokenList) {
                String tagStr = token.getStructTag().toString();
                tokenMapping.put(token.getTokenId(), tagStr);
                //get token volume
                TokenStat tokenStat = swapTxnService.getTokenVolume(tagStr, startTime, endTime);
                swapStat.setVolume(NumberUtils.getBigDecimal(swapStat.getVolume(), tokenStat.getVolume()));
                TokenTvl tvl = tokenTvlMapping.get(token.getTokenId());
                if (tvl != null) {
                    tokenStat.setTvl(tvl.getTvl());
                    tokenStat.setTvlAmount(divideScalingFactorInt(tagStr, tvl.getTvlAmount()));
                    tokenStatList.add(tokenStat);
                } else {
                    logger.warn("tvl is null: {}", token);
                }
            }

            tokenStatService.saveAll(tokenStatList);
            //get pool volume
            List<SwapPoolStat> poolStatList = new ArrayList<>();

            for (LiquidityPoolInfo poolInfo : poolInfoList) {
                LiquidityTokenId liquidityTokenId = poolInfo.getLiquidityPoolId().getLiquidityTokenId();
                String tokenA = tokenMapping.get(liquidityTokenId.getTokenXId());
                String tokenB = tokenMapping.get(liquidityTokenId.getTokenYId());
                SwapPoolStat poolStat = swapTxnService.getPoolVolume(tokenA, tokenB, startTime, endTime);
                // get pool tvl
                poolStat.setTvlA(poolInfo.getTokenXReserveInUsd());
                poolStat.setTvlB(poolInfo.getTokenYReserveInUsd());
                poolStat.setTvlAAmount(divideScalingFactorInt(tokenA, poolInfo.getTokenXReserve()));
                poolStat.setTvlBAmount(divideScalingFactorInt(tokenB, poolInfo.getTokenYReserve()));
                poolStatList.add(poolStat);
                //add for total
                BigDecimal tvl = NumberUtils.getBigDecimal(swapStat.getTvl(), poolInfo.getTokenXReserveInUsd());
                swapStat.setTvl(NumberUtils.getBigDecimal(tvl, poolInfo.getTokenYReserveInUsd()));
            }
            swapPoolStatService.saveAll(poolStatList);
            swapStatService.save(swapStat);
            logger.info("handle swap stat ok: {} - {}", startTime, endTime);

        } catch (IOException e) {
            logger.error("handle swap error:", e);
        }
    }

    public void updateTokenVolume(long startTime, long endTime) {
        //get all tokens
        List<TokenStat> tokenStatList = tokenStatService.getTokenStatByDate(startTime);
        if(tokenStatList != null && tokenStatList.size() > 0) {
            // get total stat
            SwapStat swapStat = swapStatService.get(startTime);
            BigDecimal totalVolume = null;
            if(swapStat != null) {
                totalVolume = new BigDecimal(0);
            }
            for (TokenStat tokenStat: tokenStatList) {
                // get volume
                TokenStat tokenStatNew = swapTxnService.getTokenVolume(tokenStat.getToken(), startTime, endTime);
                if(tokenStatNew != null) {
                    tokenStat.setVolume(tokenStatNew.getVolume());
                    tokenStat.setVolumeAmount(tokenStatNew.getVolumeAmount());
                    tokenStatService.save(tokenStat);
                    logger.info("update tokenStat ok: {}", tokenStat);
                    if(totalVolume != null) {
                        totalVolume = NumberUtils.getBigDecimal(totalVolume, tokenStatNew.getVolume());
                    }
                }else {
                    logger.warn("tokenStat volume null: {}, {}, {}", tokenStat.getToken(), startTime, endTime);
                }
            }

            //update total stat
            if(totalVolume != null) {
                swapStat.setVolume(totalVolume);
                swapStatService.save(swapStat);
                logger.info("update total volume to: {}, {}", totalVolume, startTime);
            }
        }else {
            logger.warn("current date token stat null: {}", startTime);
        }
    }

    public void updatePoolVolume(long startTime, long endTime) {
        Date statDate = new Date(startTime);
        List<SwapPoolStat> swapPoolStatList = swapPoolStatService.getSwapPoolStatByDate(statDate);
        if(swapPoolStatList != null && swapPoolStatList.size() > 0) {
            for (SwapPoolStat poolStat: swapPoolStatList) {
                SwapPoolStat poolStatNew = swapTxnService.getPoolVolume(poolStat.getTokenFirst(), poolStat.getTokenSecond(), startTime, endTime);
                if(poolStatNew != null) {
                    poolStat.setVolume(poolStatNew.getVolume());
                    poolStat.setVolumeAmount(poolStatNew.getVolumeAmount());
                    swapPoolStatService.save(poolStat);
                    logger.info("update pool stat volume ok: {}", poolStat);
                }else {
                    logger.warn("get volume null: {}, {}, {}, {}", poolStat.getTokenFirst(), poolStat.getTokenSecond(), startTime, endTime);
                }
            }
        }else {
            logger.warn("current date pool stat is null: {}", startTime);
        }
    }

    TotalTvl getTvls() throws JSONRPC2SessionException {
        ListResource resources = stateRPCClient.getState(contractAddress);

        Map<String, Tvl> tokenMap = new HashMap<>();
        Map<String, TokenPairTvl> tokenPairTvlMap = new HashMap<>();

        for (String key : resources.getResources().keySet()) {
            if (key.contains("Balance")) {
                long value = resources.getResources().get(key).getJson().get("token").get("value").longValue();
                String token = key.replaceFirst("0x00000000000000000000000000000001::Account::Balance<", "");
                token = token.substring(0, token.length() - 1);

                if (token.contains("LiquidityToken")) {
                    String tokenPair = token.substring(token.indexOf("<") + 1, token.length() - 1);
                    String[] tokens = tokenPair.split(",");
                    if (tokens.length != 2) {
                        continue;
                    }
                    TokenPairTvl tokenPairTvl = tvlService.getTokenPairTvl(tokens[0], tokens[1]);
                    tokenPairTvlMap.put(tokenPair.trim(), tokenPairTvl);
                } else {
                    tokenMap.put(token, new Tvl(token, value));
                }
            }
        }
        return new TotalTvl(tokenMap, tokenPairTvlMap);
    }

    void sum(Map<String, TokenStat> result, TypeTag.Struct typeTag, BigDecimal amount, OracleTokenPrice oracleTokenPrice, long ts) {
        String key = StructTagUtil.structTagToString(typeTag.value);
        TokenStat sum = result.get(key);

        TokenStat actualValue = getValue(typeTag, amount, oracleTokenPrice, ts);
        if (sum == null) {
            result.put(key, actualValue);
        } else {
            sum.add(actualValue);
        }
    }

    private TokenStat getValue(TypeTag.Struct typeTag, BigDecimal amount, OracleTokenPrice oracleTokenPrice, long ts) {
        BigDecimal actualValue = divideScalingFactor(StructTagUtil.structTagToString(typeTag.value), amount);

        BigDecimal price = oracleTokenPrice.getPrice(StructTagUtil.structTagToSwapUsdtPair(typeTag.value), ts);
        if (price == null) {
            price = BigDecimal.ZERO;
        }
        return new TokenStat(price.multiply(actualValue), actualValue, null, null);
    }

    private BigDecimal divideScalingFactor(String key, BigDecimal amount) {
        return ServiceUtils.divideScalingFactor(stateRPCClient, key, amount);
    }

    private BigInteger divideScalingFactorInt(String key, BigInteger amount) {
        BigDecimal value = ServiceUtils.divideScalingFactor(stateRPCClient, key, new BigDecimal(amount));
        return value.toBigInteger();
    }
}
