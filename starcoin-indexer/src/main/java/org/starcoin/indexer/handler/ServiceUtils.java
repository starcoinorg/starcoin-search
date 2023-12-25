package org.starcoin.indexer.handler;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.novi.bcs.BcsDeserializer;
import com.novi.serde.Bytes;
import com.novi.serde.DeserializationError;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.*;
import org.elasticsearch.cluster.metadata.MappingMetadata;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.search.SearchHit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.starcoin.api.Result;
import org.starcoin.api.StateRPCClient;
import org.starcoin.api.TransactionRPCClient;
import org.starcoin.bean.*;
import org.starcoin.bean.TransferOffset;
import org.starcoin.utils.ResultWithId;
import org.starcoin.types.ModuleId;
import org.starcoin.types.ScriptFunction;
import org.starcoin.types.StructTag;
import org.starcoin.types.TransactionPayload;
import org.starcoin.utils.*;
import org.starcoin.jsonrpc.client.JSONRPC2SessionException;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServiceUtils {

    private static final Logger logger = LoggerFactory.getLogger(ServiceUtils.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    //缓存token info
    static Map<String, TokenInfo> tokenCache = new HashMap<>();

    static {
        SimpleModule module = new SimpleModule();
        module.addDeserializer(StructTag.class, new StructTagDeserializer());
        module.addDeserializer(org.starcoin.types.TypeTag.class, new TypeTagDeserializer());
        module.addDeserializer(ModuleId.class, new ModuleDeserializer());
        module.addDeserializer(ScriptFunction.class, new ScriptFunctionDeserializer());
        module.addDeserializer(TransactionPayload.class, new TransactionPayloadDeserializer());

        module.addSerializer(TransactionPayload.class, new TransactionPayloadSerializer());
        module.addSerializer(org.starcoin.types.TypeTag.class, new TypeTagSerializer());
        module.addSerializer(StructTag.class, new StructTagSerializer());
        module.addSerializer(ScriptFunction.class, new ScriptFunctionSerializer());
        module.addSerializer(ModuleId.class, new ModuleIdSerializer());

        objectMapper.registerModule(module);
    }

    public static String getIndex(String network, String indexConstant) {
        return network + "." + indexConstant;
    }

    public static <T> Result<T> getSearchResult(SearchResponse searchResponse, Class<T> object) {
        SearchHit[] searchHit = searchResponse.getHits().getHits();
        Result<T> result = new Result<>();
        result.setTotal(searchResponse.getHits().getTotalHits().value);
        List<T> blocks = new ArrayList<>();
        for (SearchHit hit : searchHit) {
            blocks.add(JSON.parseObject(hit.getSourceAsString(), object));
        }
        result.setContents(blocks);
        return result;
    }


    public static <T> Result<T> getSearchResultJackson(SearchResponse searchResponse, Class<T> object) throws JsonProcessingException {
        SearchHit[] searchHit = searchResponse.getHits().getHits();
        Result<T> result = new Result<>();
        result.setTotal(searchResponse.getHits().getTotalHits().value);
        List<T> blocks = new ArrayList<>();
        for (SearchHit hit : searchHit) {
            blocks.add(objectMapper.readValue(hit.getSourceAsString(), object));
        }
        result.setContents(blocks);
        return result;
    }

    public static String getJsonString(Object object) throws JsonProcessingException {
        return objectMapper.writeValueAsString(object);
    }

    public static <T> ResultWithId<T> getSearchResultWithIds(SearchResponse searchResponse, Class<T> object) {
        SearchHit[] searchHit = searchResponse.getHits().getHits();
        ResultWithId<T> result = new ResultWithId<>();
        result.setTotal(searchResponse.getHits().getTotalHits().value);
        List<T> blocks = new ArrayList<>();
        List<String> ids = new ArrayList<>();
        for (SearchHit hit : searchHit) {
            blocks.add(JSON.parseObject(hit.getSourceAsString(), object));
            ids.add(hit.getId());
        }
        result.setContents(blocks);
        result.setIds(ids);
        return result;
    }

    static String createIndexIfNotExist(RestHighLevelClient client, String network, String index) throws IOException {
        String currentIndex = getIndex(network, index);
        GetIndexRequest getRequest = new GetIndexRequest(currentIndex);
        if (!client.indices().exists(getRequest, RequestOptions.DEFAULT)) {
            CreateIndexResponse response = client.indices().create(new CreateIndexRequest(currentIndex), RequestOptions.DEFAULT);
        }
        return currentIndex;
    }

    static BigInteger deserializeU128(Bytes data) {
        BcsDeserializer bcsDeserializer = new BcsDeserializer(data.content());
        try {
            return bcsDeserializer.deserialize_u128();
        } catch (DeserializationError e) {
            logger.warn("parse to u128 failed", e);
        }
        return BigInteger.ZERO;
    }

    static TransferOffset getRemoteOffset(RestHighLevelClient client, String offsetIndex) {
        GetMappingsRequest request = new GetMappingsRequest();
        try {
            request.indices(offsetIndex);
            GetMappingsResponse response = client.indices().getMapping(request, RequestOptions.DEFAULT);
            MappingMetadata data = response.mappings().get(offsetIndex);
            Object meta = data.getSourceAsMap().get("_meta");
            if (meta != null) {
                TransferOffset transferOffset = new TransferOffset();
                Map<String, Object> map = (Map<String, Object>) meta;
                String timestamp = (String) map.get("timestamp");
                Integer offset = (Integer) map.get("offset");
                transferOffset.setTimestamp(timestamp);
                transferOffset.setOffset(offset);
                return transferOffset;
            }
        } catch (Exception e) {
            logger.error("get offset error:", e);
        }
        return null;
    }

    static void setRemoteOffset(RestHighLevelClient client, String offsetIndex, TransferOffset offset) {
        PutMappingRequest request = new PutMappingRequest(offsetIndex);
        try {
            XContentBuilder builder = XContentFactory.jsonBuilder();
            builder.startObject();
            {
                builder.startObject("_meta");

                builder.field("timestamp", offset.getTimestamp());
                builder.field("offset", offset.getOffset());
                builder.endObject();
            }
            builder.endObject();
            request.source(builder);
            client.indices().putMapping(request, RequestOptions.DEFAULT);
            logger.info("remote offset update ok : {}", offset);
        } catch (Exception e) {
            logger.error("get transfer offset error:", e);
        }
    }

    static TokenInfo getTokenInfo(StateRPCClient stateRPCClient, String tokenCode) {
        TokenInfo tokenInfo = tokenCache.get(tokenCode);
        if (tokenInfo == null) {
            try {
                tokenInfo = stateRPCClient.getTokenInfo(tokenCode.substring(0, 34), tokenCode);
                if (tokenInfo != null) {
                    tokenCache.put(tokenCode, tokenInfo);
                }
            } catch (JSONRPC2SessionException | JsonProcessingException e) {
                logger.error("get token info error:", e);
            }
        }
        return tokenInfo;
    }

    public static BigDecimal divideScalingFactor(StateRPCClient stateRPCClient, String key, BigDecimal amount) {
        TokenInfo tokenInfo = ServiceUtils.getTokenInfo(stateRPCClient, key);
        BigDecimal actualValue = amount;
        if (tokenInfo != null) {
            actualValue = actualValue.movePointLeft((int) Math.log10(tokenInfo.getScalingFactor()));
        } else {
            logger.warn("token info not exist:{}", key);
        }
        return actualValue;
    }

    public static Map<String[], Long[]> getTokenReserveFromState(StateRPCClient stateRPCClient, String contractAddress, String stateRoot) throws JSONRPC2SessionException {
        ListResource resource = stateRPCClient.getState(contractAddress, true, stateRoot);
//        System.out.println(resource);
        Map<String[], Long[]> poolReserves = new HashMap<>();
        for (String key : resource.getResources().keySet()) {
            if(key.contains("TokenSwapPair")) {
                String tokenPair = key.substring(key.indexOf("<") + 1, key.length() - 1);
                String[] tokens = tokenPair.split(",");
                if (tokens.length != 2) {
                    logger.warn("state data error:", key);
                    continue;
                }
//                System.out.println("x: " + tokens[0] + ", y: " + tokens[1]);
                long xReserve = resource.getResources().get(key).getJson().get("token_x_reserve").get("value").longValue();
                long yReserve = resource.getResources().get(key).getJson().get("token_y_reserve").get("value").longValue();
//                System.out.println("x: " + xReserve + ", y: " + yReserve);
                poolReserves.put(new String[]{tokens[0].trim(), tokens[1].trim()}, new Long[]{xReserve, yReserve});
            }
        }
        return poolReserves;
    }

    static void addBlockToList(TransactionRPCClient transactionRPCClient, List<Block> blockList, Block block) throws JSONRPC2SessionException {
        List<Transaction> transactionList = transactionRPCClient.getBlockTransactions(block.getHeader().getBlockHash());
        if (transactionList == null) {
            return;
        }
        for (Transaction transaction : transactionList) {
            BlockMetadata metadata;
            Transaction userTransaction = transactionRPCClient.getTransactionByHash(transaction.getTransactionHash());
            if (userTransaction != null) {
                UserTransaction inner = userTransaction.getUserTransaction();
                metadata = userTransaction.getBlockMetadata();
                if (metadata != null) {
                    transaction.setBlockMetadata(metadata);
                    block.setBlockMetadata(metadata);
                }
                if (inner != null) {
                    try {
                        RawTransaction rawTransaction = inner.getRawTransaction();
                        TransactionPayload payload = TransactionPayload.bcsDeserialize(Hex.decode(rawTransaction.getPayload()));
                        if (TransactionPayload.Script.class.equals(payload.getClass())) {
                            transaction.setTransactionType(TransactionType.Script);
                        } else if (TransactionPayload.Package.class.equals(payload.getClass())) {
                            transaction.setTransactionType(TransactionType.Package);
                        } else if (TransactionPayload.ScriptFunction.class.equals(payload.getClass())) {
                            transaction.setTransactionType(TransactionType.ScriptFunction);
                        } else {
                            logger.warn("payload class error: {}", payload.getClass());
                        }
                        rawTransaction.setTransactionPayload(payload);
                        inner.setRawTransaction(rawTransaction);
                        transaction.setUserTransaction(inner);
                    } catch (DeserializationError deserializationError) {
                        logger.error("Deserialization payload error:", deserializationError);
                    }
                }
            } else {
                logger.warn("get txn by hash is null: {}", transaction.getTransactionHash());
            }
            transaction.setTimestamp(block.getHeader().getTimestamp());
            // set events
            List<Event> events = transactionRPCClient.getTransactionEvents(transaction.getTransactionHash());
            if (events != null && (!events.isEmpty())) {
                transaction.setEvents(events);
            } else {
                logger.warn("current txn event is null: {}", transaction.getTransactionHash());
            }
        }
        block.setTransactionList(transactionList);
        blockList.add(block);
    }
}
