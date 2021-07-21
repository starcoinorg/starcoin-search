package org.starcoin.search.handler;

import com.alibaba.fastjson.JSON;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.replication.ReplicationResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.*;
import org.elasticsearch.cluster.metadata.MappingMetadata;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.starcoin.bean.*;
import org.starcoin.search.bean.Offset;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ElasticSearchHandler {

    public static final String BLOCK_INDEX = "block_ids";
    public static final String BLOCK_CONTENT_INDEX = "blocks";
    public static final String UNCLE_BLOCK_INDEX = "uncle_blocks";
    public static final String TRANSACTION_INDEX = "txn_infos";
    public static final String EVENT_INDEX = "txn_events";
    private static final String PENDING_TXN_INDEX = "pending_txns";
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
                Map<String, Object> tip = (Map<String, Object>) ((LinkedHashMap<?, ?>) meta).get("tip");
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


    public void saveTransaction(PendingTransaction transaction) {
        if (transaction == null) {
            return;
        }
        if (!checkExists(transaction)) {
            addToEs(transaction);
        } else {
            logger.warn("transaction exist: {}", transaction.getTransactionHash());
        }

    }

    public Result<PendingTransaction> getPendingTransaction(int count) {
        SearchRequest searchRequest = new SearchRequest(ServiceUtils.getIndex(network, PENDING_TXN_INDEX));
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.matchAllQuery());
        //page size
        searchSourceBuilder.size(count);
        searchSourceBuilder.from(0);
        searchSourceBuilder.sort("timestamp", SortOrder.DESC);
        searchSourceBuilder.trackTotalHits(true);
        searchRequest.source(searchSourceBuilder);
        SearchResponse searchResponse;
        try {
            searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            logger.error("get pending transactions error:", e);
            return null;
        }
        return ServiceUtils.getSearchResult(searchResponse, PendingTransaction.class);
    }

    public void deletePendingTransaction(List<PendingTransaction> pendingTxns) {
        if (pendingTxns.size() <= 0) {
            return;
        }
        BulkRequest bulkRequest = new BulkRequest();
        String index = ServiceUtils.getIndex(network, PENDING_TXN_INDEX);
        for (PendingTransaction pending : pendingTxns) {
            DeleteRequest delete = new DeleteRequest(index);
            delete.id(pending.getTransactionHash());
            bulkRequest.add(delete);
        }
        try {
            client.bulk(bulkRequest, RequestOptions.DEFAULT);
            logger.info("delete pending transaction ok");
        } catch (IOException e) {
            logger.error("delete pending transaction error:", e);
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

        for (Block block : blockList) {
            //add block ids
            if (deleteOrSkipIndex > 0) {
                //fork block handle
                if (block.getHeader().getHeight() == deleteOrSkipIndex) {
                       logger.warn("fork block, skip: {}", deleteOrSkipIndex);
                }else {
                    //上一轮已经添加ids，需要删掉
                    DeleteRequest deleteRequest = new DeleteRequest(blockIndex);
                    deleteRequest.id(String.valueOf(deleteOrSkipIndex));
                    bulkRequest.add(deleteRequest);
                }
            }else {
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
                for (Event event : transaction.getEvents()) {
                    IndexRequest eventReq = new IndexRequest(eventIndex);
                    eventReq.id(event.getEventKey()).source(JSON.toJSONString(event), XContentType.JSON);
                    bulkRequest.add(eventReq);
                }
            }
            //add uncles
            for (BlockHeader uncle : block.getUncles()) {
                bulkRequest.add(buildUncleRequest(uncle, block.getHeader().getHeight(), uncleIndex));
            }
        }
        try {
            BulkResponse response = client.bulk(bulkRequest, RequestOptions.DEFAULT);

            logger.info("bulk block ok: {}", response.buildFailureMessage());
        } catch (IOException e) {
            logger.error("bulk block:", e);
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
        logger.debug("build: {}", Strings.toString(builder));
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
        builder.field("difficulty_number", header.getDifficulty());
        builder.field("gas_used", header.getGasUsed());
        builder.field("number", header.getHeight());
        builder.field("parent_hash", header.getParentHash());
        builder.field("state_root", header.getStateRoot());
        builder.field("txn_accumulator_root", header.getTxnAccumulatorRoot());
        builder.field("timestamp", header.getTimestamp());
    }

    private IndexRequest buildUncleRequest(BlockHeader header, long blockHeaderHeight, String indexName) {
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


    private boolean checkExists(PendingTransaction transaction) {
        try {
            GetRequest getRequest = new GetRequest(ServiceUtils.getIndex(network, TRANSACTION_INDEX), transaction.getTransactionHash());
            GetResponse getResponse = client.get(getRequest, RequestOptions.DEFAULT);
            return getResponse.isExists();
        } catch (Exception e) {
            logger.warn("access es failed", e);
            return false;
        }
    }

    private void addToEs(PendingTransaction transaction) {
        try {
            IndexRequest request = new IndexRequest(ServiceUtils.getIndex(network, PENDING_TXN_INDEX));
            request.id(transaction.getTransactionHash());

            String doc = JSON.toJSONString(transaction);
            request.source(doc, XContentType.JSON);

            IndexResponse indexResponse = null;
            try {
                indexResponse = client.index(request, RequestOptions.DEFAULT);
            } catch (ElasticsearchException e) {
                if (e.status() == RestStatus.CONFLICT) {
                    logger.error("duplicate entry\n" + e.getDetailedMessage());
                }
                logger.error("index error", e);
            }

            if (indexResponse != null) {
                if (indexResponse.getResult() == DocWriteResponse.Result.CREATED) {
                    logger.info("add transaction success: {}", transaction.getTransactionHash());
                } else if (indexResponse.getResult() == DocWriteResponse.Result.UPDATED) {
                    logger.info("update transaction success:{}", transaction.getTransactionHash());
                }
                ReplicationResponse.ShardInfo shardInfo = indexResponse.getShardInfo();
                if (shardInfo.getTotal() != shardInfo.getSuccessful()) {
                    logger.info("sharding info is " + shardInfo);
                }
            }
        } catch (IOException e) {
            logger.warn("save transaction error", e);
        }
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
