package org.starcoin.search.handler;

import com.thetransactioncompany.jsonrpc2.client.JSONRPC2SessionException;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.starcoin.api.Result;
import org.starcoin.api.StateRPCClient;
import org.starcoin.bean.ListResource;
import org.starcoin.bean.TokenInfo;
import org.starcoin.bean.Tvl;
import org.starcoin.search.bean.*;
import org.starcoin.search.constant.Constant;
import org.starcoin.search.service.SwapPoolStatService;
import org.starcoin.search.service.SwapStatService;
import org.starcoin.search.service.SwapTxnService;
import org.starcoin.search.service.TokenStatService;
import org.starcoin.search.utils.NumberUtils;
import org.starcoin.search.utils.StructTagUtil;
import org.starcoin.search.utils.SwapApiClient;
import org.starcoin.types.TypeTag;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class SwapHandle {

    private static final Logger logger = LoggerFactory.getLogger(SwapHandle.class);

    @Autowired
    private RestHighLevelClient client;

    @Value("${starcoin.network}")
    private String network;
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
        List<TokenStat> tokenStatList = new ArrayList<>();
        try {
            List<LiquidityPoolInfo> poolInfoList = swapApiClient.getPoolInfo(network);
            List<SwapToken> tokenList = swapApiClient.getTokens(network);
            if (tokenList.isEmpty()) {
                logger.warn("get token null: {}", network);
                return;
            }
            Map<String, String> tokenMapping = new HashMap<>();
            Map<String, TokenTvl> tokenTvlMapping = new HashMap<>();
            List<TokenTvl> tokenTvls = swapApiClient.getTokenTvl(network);
            for (TokenTvl tvl : tokenTvls) {
                tokenTvlMapping.put(tvl.getTokenName(), tvl);
            }
            for (SwapToken token : tokenList) {
                String tagStr = token.getStructTag().toString();
                tokenMapping.put(token.getTokenId(), tagStr);
                //get token volume
                TokenStat tokenStat = swapTxnService.getTokenVolume(tagStr, startTime, endTime);
                tokenStat.setVolumeAmount(divideScalingFactor(tagStr, tokenStat.getVolumeAmount()));
                TokenTvl tvl = tokenTvlMapping.get(token.getTokenId());
                if (tvl != null) {
                    tokenStat.setTvl(tvl.getTvl());
                    tokenStat.setTvlAmount(tvl.getTvlAmount());
                    tokenStatList.add(tokenStat);
                } else {
                    logger.warn("tvl is null: {}", token);
                }
            }

            tokenStatService.saveAll(tokenStatList);
            //get pool volume
            List<SwapPoolStat> poolStatList = new ArrayList<>();
            SwapStat swapStat = new SwapStat();
            swapStat.setStatDate(new Date(startTime));
            for (LiquidityPoolInfo poolInfo : poolInfoList) {
                LiquidityTokenId liquidityTokenId = poolInfo.getLiquidityPoolId().getLiquidityTokenId();
                String tokenA = tokenMapping.get(liquidityTokenId.getTokenXId());
                String tokenB = tokenMapping.get(liquidityTokenId.getTokenYId());
                SwapPoolStat poolStat = swapTxnService.getPoolVolume(tokenA, tokenB, startTime, endTime);
                poolStat.setVolumeAmount(divideScalingFactor(tokenA, poolStat.getVolumeAmount()));
                // get pool tvl
                poolStat.setTvlA(poolInfo.getTokenXReserveInUsd());
                poolStat.setTvlB(poolInfo.getTokenYReserveInUsd());
                poolStat.setTvlAAmount(poolInfo.getTokenXReserve());
                poolStat.setTvlBAmount(poolInfo.getTokenYReserve());
                poolStatList.add(poolStat);
                //add for total
                swapStat.setVolume(NumberUtils.getBigDecimal(swapStat.getVolume(), poolStat.getVolume()));
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

    TotalTvl getTvls() throws JSONRPC2SessionException, MalformedURLException {
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

    void sum(Map<String, TokenStat> result, TypeTag.Struct typeTag, BigDecimal amount, OracleTokenPrice oracleTokenPrice, long ts) throws NoTokenPriceException {
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
        TokenInfo tokenInfo = ServiceUtils.getTokenInfo(stateRPCClient, key);
        BigDecimal actualValue = amount;
        if (tokenInfo != null) {
            actualValue.movePointLeft((int) tokenInfo.getScalingFactor());
        } else {
            logger.warn("token info not exist:{}", key);
        }
        return actualValue;
    }

    void poolSum(Map<String, TokenPoolStat> result, TypeTag.Struct typeTagFirst, TypeTag.Struct typeTagSecond, BigDecimal amount, OracleTokenPrice oracleTokenPrice, long ts) throws NoTokenPriceException {
        String key = StructTagUtil.structTagsToTokenPair(typeTagFirst.value, typeTagSecond.value);
        TokenPoolStat sum = result.get(key);

        TokenStat xActualValue = getValue(typeTagFirst, amount, oracleTokenPrice, ts);
        TokenStat yActualValue = getValue(typeTagSecond, amount, oracleTokenPrice, ts);

        TokenPoolStat poolStat = new TokenPoolStat(xActualValue, yActualValue);

        if (sum == null) {
            result.put(key, poolStat);
        } else {
            sum.add(poolStat);
        }
    }


    Result<TransactionPayloadInfo> volumeStatsByTimeRange(long startTime, long endTime) {
        SearchRequest searchRequest = new SearchRequest(ServiceUtils.getIndex(network, Constant.PAYLOAD_INDEX));
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.size(100);

        BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery();
        queryBuilder.must(QueryBuilders.rangeQuery("timestamp").gt(startTime).lt(endTime));
        queryBuilder.should(QueryBuilders.termQuery("payload.value.function.keyword", "swap_exact_token_for_token"))
                .should(QueryBuilders.termQuery("payload.value.function.keyword", "swap_token_for_exact_token"));
        searchSourceBuilder.query(queryBuilder);

        searchSourceBuilder.from(0);
        searchSourceBuilder.trackTotalHits(true);
        searchRequest.source(searchSourceBuilder);
        searchSourceBuilder.timeout(new TimeValue(20, TimeUnit.SECONDS));
        searchSourceBuilder.sort("timestamp", SortOrder.ASC);

        try {
            SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
            return ServiceUtils.getSearchResultJackson(searchResponse, TransactionPayloadInfo.class);
        } catch (IOException e) {
            logger.error("get transfer error:", e);
            return Result.EmptyResult;
        }
    }


}
