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
import org.starcoin.bean.TokenSwapLiquidityToken;
import org.starcoin.bean.Tvl;
import org.starcoin.search.bean.*;
import org.starcoin.search.constant.Constant;
import org.starcoin.search.utils.StructTagUtil;
import org.starcoin.types.TransactionPayload;
import org.starcoin.types.TypeTag;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.starcoin.search.handler.ServiceUtils.tokenCache;

@Service
public class SwapHandle {

    private static final Logger logger = LoggerFactory.getLogger(SwapHandle.class);
    private RestHighLevelClient client;
    @Value("${starcoin.network}")
    private String network;

    private StateRPCClient starcoinClient;

    @Value("${swap.contract.address}")
    private String contractAddress;

    @Autowired
    private OracleTokenPriceService oracleTokenPriceService;

    @Autowired
    private SwapStatService swapStatService;

    static long getTimeStamp(int day) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH) + day, 0, 0, 0);
        return calendar.getTimeInMillis();
    }

    void volumeStats(long startTime, long endTime) {
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

                    BigInteger argFirst = deserializeU128(scriptFunctionPayload.value.args.get(0));
                    BigInteger argSecond = deserializeU128(scriptFunctionPayload.value.args.get(1));

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
            Map<String,Tvl> tvlMap = getTvls();

            statHashMap.forEach((k, v) -> {
                v.setToken(k);
                v.setTvl(this.moveScalingFactor(k,BigInteger.valueOf(tvlMap.get(k).getValue())));
                swapStatService.persistTokenStatInfo(v);
            });

            poolMap.forEach((k, v) -> {
                String[] tokens = k.split("/");
                v.setTokenPair(new TokenPair(tokens[0].trim(),tokens[1].trim()));
                v.setTvl(this.moveScalingFactor(k,BigInteger.valueOf(tvlMap.get(k).getValue())));
                swapStatService.persistTokenPoolStatInfo(v);
            });

        } catch (JSONRPC2SessionException e) {
            logger.error("get tvl failed ",e);
        }

    }

    Map<String, Tvl> getTvls() throws JSONRPC2SessionException {
        ListResource resources = starcoinClient.getState(contractAddress);

        Map<String, Tvl> tokenMap = new HashMap<>();

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
                    TokenSwapLiquidityToken tokenSwapLiquidityToken = new TokenSwapLiquidityToken(tokens[0], tokens[1]);
                    tokenMap.put(token, new Tvl(tokenSwapLiquidityToken, value));
                } else {
                    tokenMap.put(token, new Tvl(token, value));
                }
            }
        }
        return tokenMap;
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
        return new TokenStat(null, actualValue.multiply(price), actualValue, null);
    }

    private BigDecimal moveScalingFactor(String key, BigInteger amount) {
        TokenInfo tokenInfo = tokenCache.get(key);
        BigDecimal actualValue = new BigDecimal(amount);
        actualValue.movePointLeft((int) tokenInfo.getScalingFactor());
        return actualValue;
    }

    void poolSum(Map<String, TokenPoolStat> result, TypeTag.Struct typeTagFirst, TypeTag.Struct typeTagSecond, BigInteger amount, OracleTokenPrice oracleTokenPrice, long ts) throws NoTokenPriceException {
        String key = StructTagUtil.structTagsToTokenPair(typeTagFirst.value, typeTagSecond.value);
        TokenPoolStat sum = result.get(key);

        TokenStat actualValue = getValue(typeTagFirst, amount, oracleTokenPrice, ts);
        if (actualValue.getVolumeAmount().equals(BigDecimal.ZERO)) {
            actualValue = getValue(typeTagSecond, amount, oracleTokenPrice, ts);
        }

        if (sum == null) {
            TokenPoolStat poolStat = new TokenPoolStat(new TokenPair(typeTagFirst.value.name.value, typeTagSecond.value.name.value),
                    actualValue.getVolume(), actualValue.getVolumeAmount(), null);
            result.put(key, poolStat);
        } else {
            sum.add(actualValue);
        }
    }

    BigInteger deserializeU128(Bytes data) {
        BcsDeserializer bcsDeserializer = new BcsDeserializer(data.content());
        try {
            return bcsDeserializer.deserialize_u128();
        } catch (DeserializationError e) {
            logger.warn("parse to u128 failed", e);
        }
        return BigInteger.ZERO;
    }

    Result<TransactionPayloadInfo> volumeStatsByTimeRange(long startTime, long endTime) {
        SearchRequest searchRequest = new SearchRequest(ServiceUtils.getIndex(network, Constant.PAYLOAD_INDEX));
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.size(100);

        BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery();
        queryBuilder.must(
                QueryBuilders.boolQuery()
                        .should(QueryBuilders.rangeQuery("timestamp").gt(startTime))
                        .should(QueryBuilders.rangeQuery("timestamp").lt(endTime)));
        queryBuilder.must(
                QueryBuilders.boolQuery().should(QueryBuilders.termQuery("payload.value.function.keyword", "swap_exact_token_for_token"))
                        .should(QueryBuilders.termQuery("payload.value.function.keyword", "swap_token_for_exact_token"))
        );
        searchSourceBuilder.query(queryBuilder);

        searchSourceBuilder.from(0);
        searchSourceBuilder.trackTotalHits(true);
        searchRequest.source(searchSourceBuilder);
        searchSourceBuilder.timeout(new TimeValue(20, TimeUnit.SECONDS));
        searchSourceBuilder.sort("timestamp", SortOrder.ASC);

        try {
            SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
            return ServiceUtils.getSearchResult(searchResponse, TransactionPayloadInfo.class);
        } catch (IOException e) {
            logger.error("get transfer error:", e);
            return Result.EmptyResult;
        }
    }


}
