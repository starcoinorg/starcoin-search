package org.starcoin.search.handler;

import com.novi.bcs.BcsDeserializer;
import com.novi.serde.Bytes;
import com.novi.serde.DeserializationError;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.starcoin.api.Result;
import org.starcoin.api.StateRPCClient;
import org.starcoin.search.bean.TransactionPayloadInfo;
import org.starcoin.search.constant.Constant;
import org.starcoin.search.utils.StructTagUtil;
import org.starcoin.types.TransactionPayload;
import org.starcoin.types.TypeTag;

import java.io.IOException;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class SwapHandle {

    private static final Logger logger = LoggerFactory.getLogger(SwapHandle.class);
    private RestHighLevelClient client;
    @Value("${starcoin.network}")
    private String network;

    private StateRPCClient starcoinClient;

    @Value("${contract.address}")
    private String contractAddress;

    static long getTimeStamp(int day) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(calendar.get(Calendar.YEAR),calendar.get(Calendar.MONTH),calendar.get(Calendar.DAY_OF_MONTH)+day,0,0,0);
        return calendar.getTimeInMillis();
    }

    void volumeStats(long startTime,long endTime) {
        Set<String> handledTxn = new HashSet<>();
        Map<String, BigInteger> resultMap= new HashMap<>();
        Map<String, BigInteger> poolMap = new HashMap<>();
        long localStartTime = startTime;
        while (true) {
            Result<TransactionPayloadInfo> payloadInfoResult = volumeStatsByTimeRange(localStartTime,endTime);
            logger.info("fetch "+payloadInfoResult.getContents().size()+" from "+localStartTime + " to "+ endTime);
            for(TransactionPayloadInfo payloadInfo:payloadInfoResult.getContents()){
                if(handledTxn.contains(payloadInfo.getTransactionHash())){
                    continue;
                }
                TransactionPayload payload = payloadInfo.getPayload();
                if(payload instanceof TransactionPayload.ScriptFunction){
                    TransactionPayload.ScriptFunction scriptFunctionPayload = ((TransactionPayload.ScriptFunction) payload);
                    if(scriptFunctionPayload.value.ty_args.size()<2){
                        continue;
                    }
                    if(!(scriptFunctionPayload.value.ty_args.get(0) instanceof TypeTag.Struct)){
                        continue;
                    }
                    if(!(scriptFunctionPayload.value.ty_args.get(1) instanceof TypeTag.Struct)){
                        continue;
                    }

                    if(scriptFunctionPayload.value.args.size()<2){
                        continue;
                    }
                    TypeTag.Struct typeTagFirst = (TypeTag.Struct) scriptFunctionPayload.value.ty_args.get(0);
                    TypeTag.Struct typeTagSecond = (TypeTag.Struct) scriptFunctionPayload.value.ty_args.get(1);

                    BigInteger argFirst = deserializeU128(scriptFunctionPayload.value.args.get(0));
                    BigInteger argSecond = deserializeU128(scriptFunctionPayload.value.args.get(1));

                    sum(resultMap,typeTagFirst,argFirst);
                    sum(resultMap,typeTagSecond,argSecond);

                    poolSum(poolMap,typeTagFirst,typeTagSecond,argFirst);

                    handledTxn.add(payloadInfo.getTransactionHash());
                    if(localStartTime < payloadInfo.getTimestamp()) {
                        localStartTime = payloadInfo.getTimestamp();
                    }
                }
            }
            if(payloadInfoResult.getContents().size()==0){
                break;
            }
        }

    }



    void sum(Map<String,BigInteger> result,TypeTag.Struct typeTag,BigInteger amount){
        String key = StructTagUtil.structTagToString(typeTag.value);
        BigInteger sum = result.get(key);
        if (sum == null) {
            result.put(key,amount);
        }else {
            sum.add(amount);
        }
    }

    void poolSum(Map<String,BigInteger> result,TypeTag.Struct typeTagFirst,TypeTag.Struct typeTagSecond,BigInteger amount){
        String key = StructTagUtil.structTagsToTokenPair(typeTagFirst.value,typeTagSecond.value);
        BigInteger sum = result.get(key);
        if (sum == null) {
            result.put(key,amount);
        }else {
            sum.add(amount);
        }
    }

    BigInteger deserializeU128(Bytes data){
        BcsDeserializer bcsDeserializer = new BcsDeserializer(data.content());
        try {
            return bcsDeserializer.deserialize_u128();
        } catch (DeserializationError e) {
            logger.warn("parse to u128 failed",e);
        }
        return BigInteger.ZERO;
    }

    Result<TransactionPayloadInfo> volumeStatsByTimeRange(long startTime,long endTime){
        SearchRequest searchRequest = new SearchRequest(ServiceUtils.getIndex(network, Constant.PAYLOAD_INDEX));
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.size(100);

        BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery();
        queryBuilder.must(
                        QueryBuilders.boolQuery()
                                .should(QueryBuilders.rangeQuery("timestamp").gt(startTime))
                                .should(QueryBuilders.rangeQuery("timestamp").lt(endTime)));
        queryBuilder.must(
                QueryBuilders.boolQuery().should(QueryBuilders.termQuery("payload.value.function.keyword","swap_exact_token_for_token"))
                        .should(QueryBuilders.termQuery("payload.value.function.keyword","swap_token_for_exact_token"))
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
