package org.starcoin.search.handler;

import com.novi.bcs.BcsDeserializer;
import com.novi.serde.Bytes;
import com.novi.serde.DeserializationError;
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
import org.starcoin.search.utils.StructTagUtil;
import org.starcoin.types.TransactionPayload;
import org.starcoin.types.TypeTag;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class SwapHandle {

    private static final Logger logger = LoggerFactory.getLogger(SwapHandle.class);

    @Autowired
    private RestHighLevelClient client;

    @Value("${starcoin.network}")
    private String network;

    @Autowired
    private StateRPCClient stateRPCClient;

    @Value("${swap.contract.address}")
    private String contractAddress;

    @Value("${node.address}")
    private String nodeAddress;

    @Autowired
    private OracleTokenPriceService oracleTokenPriceService;

    @Autowired
    private SwapStatService swapStatService;

    public void volumeStats(long startTime, long endTime) {
        Set<String> handledTxn = new HashSet<>();
        Map<String, TokenStat> statHashMap = new HashMap<>();
        Map<String, TokenPoolStat> poolMap = new HashMap<>();
        long localStartTime = startTime;

        // sum volume and volume amount in txn
        while (true) {
            Result<TransactionPayloadInfo> payloadInfoResult = volumeStatsByTimeRange(localStartTime, endTime);
            logger.info("fetch " + payloadInfoResult.getContents().size() + " from " + localStartTime + " to " + endTime);

            if (payloadInfoResult.getContents().size() == 0) {
                break;
            }

            long localEndTime = 0;
            for (TransactionPayloadInfo payloadInfo : payloadInfoResult.getContents()) {
                if (payloadInfo.getTimestamp() > localEndTime) {
                    localEndTime = payloadInfo.getTimestamp();
                }
            }

            OracleTokenPrice oracleTokenPrice = oracleTokenPriceService.getPriceByTimeRange(localStartTime, localEndTime);

            for (TransactionPayloadInfo payloadInfo : payloadInfoResult.getContents()) {
                if (handledTxn.contains(payloadInfo.getTransactionHash())) {
                    continue;
                }
                TransactionPayload payload = payloadInfo.getPayload();
                if (payload instanceof TransactionPayload.ScriptFunction) {
                    TransactionPayload.ScriptFunction scriptFunctionPayload = ((TransactionPayload.ScriptFunction) payload);
                    if (scriptFunctionPayload.value.ty_args.size() < 2) {
                        continue;
                    }
                    if (!(scriptFunctionPayload.value.ty_args.get(0) instanceof TypeTag.Struct)) {
                        continue;
                    }
                    if (!(scriptFunctionPayload.value.ty_args.get(1) instanceof TypeTag.Struct)) {
                        continue;
                    }

                    if (scriptFunctionPayload.value.args.size() < 2) {
                        continue;
                    }
                    TypeTag.Struct typeTagFirst = (TypeTag.Struct) scriptFunctionPayload.value.ty_args.get(0);
                    TypeTag.Struct typeTagSecond = (TypeTag.Struct) scriptFunctionPayload.value.ty_args.get(1);

                    BigInteger argFirst = ServiceUtils.deserializeU128(scriptFunctionPayload.value.args.get(0));
                    BigInteger argSecond = ServiceUtils.deserializeU128(scriptFunctionPayload.value.args.get(1));

                    try {
                        sum(statHashMap, typeTagFirst, argFirst, oracleTokenPrice, payloadInfo.getTimestamp());
                        sum(statHashMap, typeTagSecond, argSecond, oracleTokenPrice, payloadInfo.getTimestamp());

                        poolSum(poolMap, typeTagFirst, typeTagSecond, argFirst, oracleTokenPrice, payloadInfo.getTimestamp());

                    } catch (NoTokenPriceException e) {
                        logger.error("no price in oracle,skip it .");
                    }

                    handledTxn.add(payloadInfo.getTransactionHash());
                    if (localStartTime < payloadInfo.getTimestamp()) {
                        localStartTime = payloadInfo.getTimestamp();
                    }
                }
            }
        }

        // get tvl from contract
        try {
            TotalTvl totalTvl = getTvls();

            statHashMap.forEach((k, v) -> {
                v.setToken(k);
                v.setTvlAmount(this.moveScalingFactor(k,BigInteger.valueOf(totalTvl.getTokenTvlMap().get(k).getValue())));

                BigDecimal price = oracleTokenPriceService.getPriceByTimeRangeAndToken(startTime,endTime,k);
                v.setTvl(v.getTvlAmount().multiply(price));

                swapStatService.persistTokenStatInfo(v);
            });

            poolMap.forEach((k, v) -> {
                v.getxStats().setTvlAmount(this.moveScalingFactor(v.getxStats().getToken(),v.getxStats().getTvlAmount().toBigInteger()));
                v.getyStats().setTvlAmount(this.moveScalingFactor(v.getyStats().getToken(),v.getyStats().getTvlAmount().toBigInteger()));

                BigDecimal priceX = oracleTokenPriceService.getPriceByTimeRangeAndToken(startTime,endTime,v.getxStats().getToken());
                BigDecimal priceY = oracleTokenPriceService.getPriceByTimeRangeAndToken(startTime,endTime,v.getyStats().getToken());

                v.getxStats().setTvl(priceX.multiply(v.getxStats().getTvlAmount()));
                v.getyStats().setTvl(priceY.multiply(v.getyStats().getTvlAmount()));

                swapStatService.persistTokenPoolStatInfo(v);
            });

        } catch (JSONRPC2SessionException | MalformedURLException e) {
            logger.error("get tvl failed ",e);
        }

    }

    TotalTvl getTvls() throws JSONRPC2SessionException, MalformedURLException {
        ListResource resources = stateRPCClient.getState(contractAddress);

        Map<String, Tvl> tokenMap = new HashMap<>();
        Map<String, TokenPairTvl> tokenPairTvlMap = new HashMap<>();

        TvlService tvlService =new TvlService(new URL(nodeAddress));

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
                    TokenPairTvl tokenPairTvl = tvlService.getTokenPairTvl(tokens[0],tokens[1]);
                    tokenPairTvlMap.put(tokenPair.trim(),tokenPairTvl);
                } else {
                    tokenMap.put(token, new Tvl(token, value));
                }
            }
        }
        return new TotalTvl(tokenMap,tokenPairTvlMap);
    }

    void sum(Map<String, TokenStat> result, TypeTag.Struct typeTag, BigInteger amount, OracleTokenPrice oracleTokenPrice, long ts) throws NoTokenPriceException {
        String key = StructTagUtil.structTagToString(typeTag.value);
        TokenStat sum = result.get(key);

        TokenStat actualValue = getValue(typeTag, amount, oracleTokenPrice, ts);
        if (sum == null) {
            result.put(key, actualValue);
        } else {
            sum.add(actualValue);
        }
    }

    private TokenStat getValue(TypeTag.Struct typeTag, BigInteger amount, OracleTokenPrice oracleTokenPrice, long ts) {
        BigDecimal actualValue = moveScalingFactor(StructTagUtil.structTagToString(typeTag.value), amount);

        BigDecimal price = oracleTokenPrice.getPrice(StructTagUtil.structTagToSwapUsdtPair(typeTag.value), ts);
        if (price == null) {
            price = BigDecimal.ZERO;
        }
        return new TokenStat(null, actualValue.multiply(price), actualValue, null,null);
    }

    private BigDecimal moveScalingFactor(String key, BigInteger amount) {
        TokenInfo tokenInfo = ServiceUtils.getTokenInfo(stateRPCClient, key);
        BigDecimal actualValue = new BigDecimal(amount);
        if (tokenInfo != null) {
            actualValue.movePointLeft((int) tokenInfo.getScalingFactor());
        } else {
            logger.warn("token info not exist:{}", key);
        }
        return actualValue;
    }

    void poolSum(Map<String, TokenPoolStat> result, TypeTag.Struct typeTagFirst, TypeTag.Struct typeTagSecond, BigInteger amount, OracleTokenPrice oracleTokenPrice, long ts) throws NoTokenPriceException {
        String key = StructTagUtil.structTagsToTokenPair(typeTagFirst.value, typeTagSecond.value);
        TokenPoolStat sum = result.get(key);

        TokenStat xActualValue = getValue(typeTagFirst, amount, oracleTokenPrice, ts);
        TokenStat yActualValue = getValue(typeTagSecond, amount, oracleTokenPrice, ts);

        TokenPoolStat poolStat = new TokenPoolStat(xActualValue,yActualValue);

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
