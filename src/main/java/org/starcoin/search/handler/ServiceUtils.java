package org.starcoin.search.handler;

import com.alibaba.fastjson.JSON;
import com.novi.serde.DeserializationError;
import com.thetransactioncompany.jsonrpc2.client.JSONRPC2SessionException;
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
import org.starcoin.api.TransactionRPCClient;
import org.starcoin.bean.*;
import org.starcoin.search.bean.TransferOffset;
import org.starcoin.types.TransactionPayload;
import org.starcoin.utils.Hex;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ServiceUtils {

    private static final Logger logger = LoggerFactory.getLogger(ServiceUtils.class);

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

    static void createIndexIfNotExist(RestHighLevelClient client, String network, String index) throws IOException {
        String currentIndex = getIndex(network, index);
        GetIndexRequest getRequest = new GetIndexRequest(currentIndex);
        if (!client.indices().exists(getRequest, RequestOptions.DEFAULT)) {
            CreateIndexResponse response = client.indices().create(new CreateIndexRequest(currentIndex), RequestOptions.DEFAULT);
        }
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

    static void addBlockToList(TransactionRPCClient transactionRPCClient, List<Block> blockList, Block block) throws JSONRPC2SessionException {
        List<Transaction> transactionList = transactionRPCClient.getBlockTransactions(block.getHeader().getBlockHash());
        if (transactionList == null) {
            return;
        }
        for (Transaction transaction : transactionList) {
            BlockMetadata metadata = null;
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
