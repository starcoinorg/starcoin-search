package org.starcoin.search.handler;

import com.alibaba.fastjson.JSON;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.*;
import org.elasticsearch.cluster.metadata.MappingMetadata;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.starcoin.bean.*;
import org.starcoin.search.bean.Offset;
import org.starcoin.types.StructTag;
import org.starcoin.utils.Hex;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ElasticSearchHandler {

    private static final String BLOCK_INDEX = "block_ids";
    private static final String BLOCK_CONTENT_INDEX = "blocks";
    private static final String UNCLE_BLOCK_INDEX = "uncle_blocks";
    private static final String TRANSACTION_INDEX = "txn_infos";
    private static final String EVENT_INDEX = "txn_events";
    private static final String PENDING_TXN_INDEX = "pending_txns";
    private static final String TRANSFER_INDEX = "transfer";
    private static final int ELASTICSEARCH_MAX_HITS = 10000;

    private static final Logger logger = LoggerFactory.getLogger(ElasticSearchHandler.class);
    private final RestHighLevelClient client;
    @Value("${starcoin.network}")
    private String network;

    public ElasticSearchHandler(RestHighLevelClient client) {
        this.client = client;
    }

    @PostConstruct
    public void initIndexes() {
        logger.info("init indices...");
        try {
            createIndexIfNotExist(BLOCK_INDEX);
            createIndexIfNotExist(BLOCK_CONTENT_INDEX);
            createIndexIfNotExist(UNCLE_BLOCK_INDEX);
            createIndexIfNotExist(TRANSACTION_INDEX);
            createIndexIfNotExist(EVENT_INDEX);
            createIndexIfNotExist(PENDING_TXN_INDEX);
            createIndexIfNotExist(TRANSFER_INDEX);
            logger.info("index init ok!");
        } catch (IOException e) {
            logger.error("init index error:", e);
        }
    }

    public Offset getRemoteOffset() {
        GetMappingsRequest request = new GetMappingsRequest();
        try {
            String offsetIndex = ServiceUtils.getIndex(network, BLOCK_CONTENT_INDEX);
            request.indices(offsetIndex);
            GetMappingsResponse response = client.indices().getMapping(request, RequestOptions.DEFAULT);
            MappingMetadata data = response.mappings().get(offsetIndex);
            Object meta = data.getSourceAsMap().get("_meta");
            if (meta != null) {
                Map<String, Object> tip = (Map<String, Object>) ((LinkedHashMap<String, Object>) meta).get("tip");
                String blockHash = tip.get("block_hash").toString();
                Integer blockHeight = (Integer) tip.get("block_number");
                return new Offset(blockHeight.longValue(), blockHash);
            }
        } catch (Exception e) {
            logger.error("get remote offset error:", e);
        }
        return null;
    }

    public void setRemoteOffset(Offset offset) {
        String offsetIndex = ServiceUtils.getIndex(network, BLOCK_CONTENT_INDEX);
        PutMappingRequest request = new PutMappingRequest(offsetIndex);
        try {
            XContentBuilder builder = XContentFactory.jsonBuilder();
            builder.startObject();
            {
                builder.startObject("_meta");
                {
                    builder.startObject("tip");
                    {
                        builder.field("block_hash", offset.getBlockHash());
                        builder.field("block_number", offset.getBlockHeight());
                    }
                    builder.endObject();
                }
                builder.endObject();
            }
            builder.endObject();
            request.source(builder);
            client.indices().putMapping(request, RequestOptions.DEFAULT);
            logger.info("remote offset update ok : {}", offset);
        } catch (Exception e) {
            logger.error("get remote offset error:", e);
        }
    }


    public void bulk(List<Block> blockList, long deleteOrSkipIndex) {
        if (blockList.isEmpty()) {
            logger.warn("block list is empty");
            return;
        }
        BulkRequest bulkRequest = new BulkRequest();
        String blockIndex = ServiceUtils.getIndex(network, BLOCK_INDEX);
        String blockContentIndex = ServiceUtils.getIndex(network, BLOCK_CONTENT_INDEX);
        String uncleIndex = ServiceUtils.getIndex(network, UNCLE_BLOCK_INDEX);
        String txnIndex = ServiceUtils.getIndex(network, TRANSACTION_INDEX);
        String eventIndex = ServiceUtils.getIndex(network, EVENT_INDEX);
        String pendingIndex = ServiceUtils.getIndex(network, PENDING_TXN_INDEX);
        String transferIndex = ServiceUtils.getIndex(network, TRANSFER_INDEX);
        for (Block block : blockList) {
            //transform difficulty
            BlockHeader header = block.getHeader();
            transferDifficulty(header);
            block.setHeader(header);
            //add block ids
            if (deleteOrSkipIndex > 0) {
                //fork block handle
                if (header.getHeight() == deleteOrSkipIndex) {
                    logger.warn("fork block, skip: {}", deleteOrSkipIndex);
                } else {
                    //上一轮已经添加ids，需要删掉
                    DeleteRequest deleteRequest = new DeleteRequest(blockIndex);
                    deleteRequest.id(String.valueOf(deleteOrSkipIndex));
                    bulkRequest.add(deleteRequest);
                }
            } else {
                bulkRequest.add(buildBlockRequest(block, blockIndex));
            }
            //  add block content
            IndexRequest blockContent = new IndexRequest(blockContentIndex);
            blockContent.id(block.getHeader().getBlockHash()).source(JSON.toJSONString(block), XContentType.JSON);
            bulkRequest.add(blockContent);
            //add txns
            for (Transaction transaction : block.getTransactionList()) {
                IndexRequest transactionReq = new IndexRequest(txnIndex);
                transactionReq.id(transaction.getTransactionHash()).source(JSON.toJSONString(transaction), XContentType.JSON);
                bulkRequest.add(transactionReq);
                //delete pending txn
                DeleteRequest deleteRequest = new DeleteRequest(pendingIndex);
                deleteRequest.id(transaction.getTransactionHash());
                bulkRequest.add(deleteRequest);
                //add events
                List<Event> events = transaction.getEvents();
                if (events != null && events.size() > 0) {
                    for (Event event : events) {
                        bulkRequest.add(buildEventRequest(event, transaction.getTimestamp(), eventIndex));
                    }
                }
                //add transfer
                IndexRequest transferRequest = buildTransferRequest(transaction, transferIndex);
                if (transferRequest != null) {
                    bulkRequest.add(transferRequest);
                }
            }
            //add uncles
            for (BlockHeader uncle : block.getUncles()) {
                bulkRequest.add(buildUncleRequest(uncle, header.getHeight(), uncleIndex));
            }
        }
        try {
            BulkResponse response = client.bulk(bulkRequest, RequestOptions.DEFAULT);
            logger.info("bulk block result: {}", response.buildFailureMessage());
        } catch (IOException e) {
            logger.error("bulk block error:", e);
        }
    }

    private void transferDifficulty(BlockHeader header) {
        String difficultyStr = header.getDifficultyHexStr();
        if (difficultyStr.startsWith("0x")) {
            try {
                long difficulty = Long.parseLong(difficultyStr.substring(2), 16);
                if (difficulty > 0) {
                    header.setDifficulty(difficulty);
                }
            } catch (NumberFormatException e) {
                logger.error("transform difficulty error: {}", difficultyStr);
            }
        }
    }

    private IndexRequest buildBlockRequest(Block bLock, String indexName) {
        IndexRequest request = new IndexRequest(indexName);
        XContentBuilder builder = null;
        BlockHeader header = bLock.getHeader();
        BlockMetadata metadata = bLock.getBlockMetadata();
        try {
            builder = XContentFactory.jsonBuilder();
            builder.startObject();
            {
                builder.field("timestamp", header.getTimestamp());
                builder.startObject("header");
                {
                    blockHeaderBuilder(builder, header);
                }
                builder.endObject();
                if (metadata != null) {
                    builder.startObject("metadata");
                    {
                        builder.field("author", metadata.getAuthor());
                        builder.field("parentHash", metadata.getParentHash());
                        builder.field("timestamp", metadata.getTimestamp());
                        builder.field("author_auth_key", metadata.getAuthorAuthKey());
                        builder.field("uncles", metadata.getUncles());
                        builder.field("number", metadata.getNumber());
                        builder.field("chain_id", header.getChainId());
                        builder.field("parent_gas_used", metadata.getParentGasUsed());
                    }
                    builder.endObject();
                }
            }
            builder.endObject();
        } catch (IOException e) {
            logger.error("build block error:", e);
        }
        request.id(String.valueOf(bLock.getHeader().getHeight())).source(builder);
        return request;
    }

    private void blockHeaderBuilder(XContentBuilder builder, BlockHeader header) throws IOException {
        builder.field("author", header.getAuthor());
        builder.field("author_auth_key", header.getAuthorAuthKey());
        builder.field("block_accumulator_root", header.getBlockAccumulatorRoot());
        builder.field("block_hash", header.getBlockHash());
        builder.field("body_hash", header.getBodyHash());
        builder.field("chain_id", header.getChainId());
        builder.field("difficulty", header.getDifficultyHexStr());
        builder.field("difficulty_number", header.getDifficulty());
        builder.field("gas_used", header.getGasUsed());
        builder.field("number", header.getHeight());
        builder.field("parent_hash", header.getParentHash());
        builder.field("state_root", header.getStateRoot());
        builder.field("txn_accumulator_root", header.getTxnAccumulatorRoot());
        builder.field("timestamp", header.getTimestamp());
    }

    private IndexRequest buildUncleRequest(BlockHeader header, long blockHeaderHeight, String indexName) {
        //transfer difficulty
        transferDifficulty(header);
        IndexRequest request = new IndexRequest(indexName);
        XContentBuilder builder = null;
        try {
            builder = XContentFactory.jsonBuilder();
            builder.startObject();
            {
                builder.startObject("header");
                {
                    blockHeaderBuilder(builder, header);
                }
                builder.endObject();
                builder.field("uncle_block_number", blockHeaderHeight);
            }
            builder.endObject();
        } catch (IOException e) {
            logger.error("build block error:", e);
        }
        request.source(builder);
        return request;
    }

    private IndexRequest buildTransferRequest(Transaction transaction, String indexName) {
        UserTransaction userTransaction = transaction.getUserTransaction();
        if (userTransaction == null) {
            return null;
        }
        RawTransaction rawTransaction = userTransaction.getRawTransaction();
        if (rawTransaction == null) {
            return null;
        }
        org.starcoin.types.TransactionPayload payload = rawTransaction.getTransactionPayload();
        if (!(payload.getClass() == org.starcoin.types.TransactionPayload.ScriptFunction.class)) {
            //todo script and package handle
            logger.warn("other type must handle in future: {}", payload.getClass());
            return null;
        }
        org.starcoin.types.ScriptFunction function = ((org.starcoin.types.TransactionPayload.ScriptFunction) payload).value;
        if (function.function.value.equals("peer_to_peer") || function.function.value.equals("peer_to_peer_v2")) {
            Transfer transfer = new Transfer();
            transfer.setTxnHash(transaction.getTransactionHash());
            transfer.setSender(rawTransaction.getSender());
            transfer.setTimestamp(transaction.getTimestamp());
            transfer.setIdentifier(function.function.value);
            transfer.setReceiver(Hex.encode(function.args.get(0)));
            String amount = Hex.encode(function.args.get(1));
            if (function.function.value.equals("peer_to_peer")) {
                amount = Hex.encode(function.args.get(2));
            }
            transfer.setAmount(amount);
            transfer.setTypeTag(getTypeTags(function.ty_args));
            IndexRequest request = new IndexRequest(indexName);
            request.source(JSON.toJSONString(transfer), XContentType.JSON);
            return request;
        } else {
            logger.warn("other scripts not support: {}", function.function.value);
            return null;
        }
    }

    private String getTypeTags(List<org.starcoin.types.TypeTag> typeTags) {
        if (typeTags.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for( org.starcoin.types.TypeTag typeTag: typeTags) {
            if (typeTag.getClass()== org.starcoin.types.TypeTag.Struct.class ) {
                StructTag structTag = ((org.starcoin.types.TypeTag.Struct) typeTag).value;
                sb.append(structTag.address).append("::").append(structTag.module).append("::").append(structTag.name).append(";");
            }
        }
        if(sb.length() > 1) {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }

    private IndexRequest buildEventRequest(Event event, long timestamp, String indexName) {
        IndexRequest request = new IndexRequest(indexName);
        XContentBuilder builder = null;
        try {
            Struct struct = Struct.fromRPC(event.getTypeTag());
            builder = XContentFactory.jsonBuilder();
            builder.startObject();
            builder.field("event_seq_number", event.getEventSeqNumber());
            builder.field("block_hash", event.getBlockHash());
            builder.field("block_number", event.getBlockNumber());
            builder.field("transaction_hash", event.getTransactionHash());
            builder.field("transaction_index", event.getTransactionIndex());
            builder.field("data", event.getData());
            builder.field("type_tag", event.getTypeTag());
            builder.field("event_key", event.getEventKey());
            builder.field("event_address", event.eventCreateAddress());
            if (struct != null) {
                builder.field("tag_address", struct.getAddress());
                builder.field("tag_module", struct.getModule());
                builder.field("tag_name", struct.getName());
            } else {
                logger.warn("type tag is not struct: {}", event.getTypeTag());
            }
            builder.field("timestamp", timestamp);
            builder.endObject();
        } catch (IOException e) {
            logger.error("build block error:", e);
        }
        request.source(builder);
        return request;
    }

    private void createIndexIfNotExist(String index) throws IOException {
        String currentIndex = ServiceUtils.getIndex(network, index);
        GetIndexRequest getRequest = new GetIndexRequest(currentIndex);
        if (!client.indices().exists(getRequest, RequestOptions.DEFAULT)) {
            CreateIndexResponse response = client.indices().create(new CreateIndexRequest(currentIndex), RequestOptions.DEFAULT);
            logger.info("create index response: {}", response.toString());
        }
    }
}
