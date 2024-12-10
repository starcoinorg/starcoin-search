package org.starcoin.indexer.handler;

import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.starcoin.api.Result;
import org.starcoin.api.TransactionRPCClient;
import org.starcoin.bean.Transaction;
import org.starcoin.constant.Constant;
import com.thetransactioncompany.jsonrpc2.client.JSONRPC2SessionException;

import java.io.IOException;
import java.util.List;

@Service
public class TransactionInfoHandle {

    private static final Logger logger = LoggerFactory.getLogger(TransactionInfoHandle.class);
    private final RestHighLevelClient client;
    @Value("${starcoin.indexer.txn_offset}")
    private long globalIndex;
    @Value("${starcoin.network}")
    private String network;

    @Autowired
    private TransactionRPCClient transactionRPCClient;

    public TransactionInfoHandle(RestHighLevelClient client) {
        this.client = client;
    }

    protected void handle() {
        //read offset
        logger.info("transaction info handle offset:" + globalIndex);
        try {
            List<Transaction> transactionList = getTransactionByGlobalIndex(globalIndex, 100);
            if(transactionList != null && transactionList.size() > 0) {
                long index = globalIndex;
                BulkRequest bulkRequest = new BulkRequest();
                for (Transaction transaction: transactionList) {
                    addUpdateRequest(transaction.getTransactionHash(), transaction.getTransactionGlobalIndex(), bulkRequest);
                    if(index > transaction.getTransactionGlobalIndex()) {
                        index = transaction.getTransactionGlobalIndex();
                    }
                }
                client.bulk(bulkRequest, RequestOptions.DEFAULT);
                //update offset
                this.globalIndex = index;
                logger.info("transaction info handle ok:" + globalIndex);
            }
        } catch (Exception e) {
            logger.error("transaction info handle err:", e);
        }
    }

    //get transaction range from offset
    public Result getTransaction(long offset, int count) {
        SearchRequest searchRequest = new SearchRequest(ServiceUtils.getIndex(network, Constant.TRANSACTION_INDEX));
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        //page size
        searchSourceBuilder.size(count);
        searchSourceBuilder.from(0);
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        boolQueryBuilder.must(QueryBuilders.rangeQuery("transaction_global_index").gt(offset));

        searchSourceBuilder.query(boolQueryBuilder);
        searchRequest.source(searchSourceBuilder);
        searchSourceBuilder.trackTotalHits(true);
        SearchResponse searchResponse;
        try {
            searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            logger.error("get transaction error:", e);
            return Result.EmptyResult;
        }
        return ServiceUtils.getSearchResultWithIds(searchResponse, Transaction.class);
    }

    //get transaction list of global index is null
    public Result getGlobalIndexNullTransaction(int count) {
        SearchRequest searchRequest = new SearchRequest(ServiceUtils.getIndex(network, Constant.TRANSACTION_INDEX));
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        //page size
        searchSourceBuilder.size(count);
        searchSourceBuilder.from(0);
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        boolQueryBuilder.mustNot(QueryBuilders.existsQuery("transaction_global_index"));

        searchSourceBuilder.query(boolQueryBuilder);
        searchRequest.source(searchSourceBuilder);
        searchSourceBuilder.trackTotalHits(true);
        SearchResponse searchResponse;
        try {
            searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            logger.error("get transaction error:", e);
            return Result.EmptyResult;
        }
        return ServiceUtils.getSearchResultWithIds(searchResponse, Transaction.class);
    }

    public List<Transaction> getTransactionByGlobalIndex(long globalIndex, int count) throws JSONRPC2SessionException {
        return transactionRPCClient.getTransactionInfos(globalIndex, true, count);
    }

    private void addUpdateRequest(String id, long globalIndex, BulkRequest bulkRequest) {
        try {
            XContentBuilder builder = XContentFactory.jsonBuilder();
            builder.startObject();
            {
                builder.field("transaction_global_index", globalIndex);
            }
            builder.endObject();
            IndexRequest indexRequest = new IndexRequest(Constant.TRANSACTION_INDEX);
            indexRequest.id(id).source(builder);
            UpdateRequest updateRequest = new UpdateRequest();
            updateRequest.index(Constant.TRANSACTION_INDEX);
            updateRequest.id(id);
            updateRequest.doc(builder);
            updateRequest.upsert(indexRequest);
            bulkRequest.add(updateRequest);
        } catch (IOException e) {
            logger.error("update request error:", e);
        }
    }
}
