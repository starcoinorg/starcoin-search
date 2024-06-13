package org.starcoin.scan.service;

import com.alibaba.fastjson.JSON;
import com.beust.jcommander.internal.Lists;
import com.novi.serde.DeserializationError;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.metrics.ParsedValueCount;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.starcoin.api.Result;
import org.starcoin.bean.*;
import org.starcoin.constant.Constant;
import org.starcoin.scan.service.vo.TransactionWithEvent;
import org.starcoin.types.AccountAddress;
import org.starcoin.types.event.ProposalCreatedEvent;
import org.starcoin.utils.ByteUtils;

import java.io.IOException;
import java.util.*;

import static org.starcoin.scan.service.ServiceUtils.ELASTICSEARCH_MAX_HITS;
import static org.starcoin.scan.service.ServiceUtils.getSearchUnescapeResult;


@Service
public class TransactionService extends BaseService {
    private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);

    @Autowired
    private RestHighLevelClient client;

    public TransactionWithEvent get(String network, String id) throws IOException {
        GetRequest getRequest = new GetRequest(getIndex(network, Constant.TRANSACTION_INDEX), id);
        GetResponse getResponse = client.get(getRequest, RequestOptions.DEFAULT);
        if (getResponse.isExists()) {
            String sourceAsString = getResponse.getSourceAsString();
            TransactionWithEvent transaction = JSON.parseObject(sourceAsString, TransactionWithEvent.class);
            // is not forked block
            if (isNotForkedBlock(network, transaction.getBlockHash())) {
                //get events
                List<String> txnHashes = new ArrayList<>();
                txnHashes.add(transaction.getTransactionHash());
                Result<Event> events = getEventsByTransaction(network, txnHashes);
                transaction.setEvents(events.getContents());
                return transaction;
            } else {
                logger.warn("is forked block txns: {}", id);
                return null;
            }
        } else {
            logger.error("not found transaction, id: {}", id);
            return null;
        }
    }

    private boolean isNotForkedBlock(String network, String blockHash) {
        SearchRequest searchRequest = new SearchRequest(getIndex(network, Constant.BLOCK_IDS_INDEX));
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        TermQueryBuilder termQueryBuilder = QueryBuilders.termQuery("header.block_hash", blockHash);
        searchSourceBuilder.query(termQueryBuilder);
        searchRequest.source(searchSourceBuilder);
        SearchResponse searchResponse;
        try {
            searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            logger.error("get master block by hash error:", e);
            return false;
        }
        return searchResponse.getHits().getHits().length > 0;
    }

    public TransactionWithEvent getTransactionByHash(String network, String hash) throws IOException {
        return get(network, hash);
    }

    public Result<TransactionWithEvent> getRange(String network, int page, int count, int start_height, int txnType) throws IOException {
        SearchRequest searchRequest = new SearchRequest(getIndex(network, Constant.TRANSACTION_INDEX));
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        if (txnType == 0)//
            searchSourceBuilder.query(QueryBuilders.matchAllQuery());
        else
            searchSourceBuilder.query(QueryBuilders.rangeQuery("transaction_index").gt(0));
        //page size
        searchSourceBuilder.size(count);
        //begin offset
        setBlockSearchBuildFrom(page, count, start_height, searchSourceBuilder);

        searchSourceBuilder.sort("timestamp", SortOrder.DESC);
        searchRequest.source(searchSourceBuilder);
        searchSourceBuilder.trackTotalHits(true);
        SearchResponse searchResponse = null;
        try {
            searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        } catch (ElasticsearchException e) {
            logger.error("get txn err:", e);
            return  Result.EmptyResult;
        }
        return ServiceUtils.getSearchResult(searchResponse, TransactionWithEvent.class);
    }

    public Result<TransactionWithEvent> getTxnByStartTime(String network, long start_time, int page, int count, int txnType) throws IOException {
        SearchRequest searchRequest = new SearchRequest(getIndex(network, Constant.TRANSACTION_INDEX));
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        if (txnType == 0)//
            searchSourceBuilder.query(QueryBuilders.matchAllQuery());
        else
            searchSourceBuilder.query(QueryBuilders.rangeQuery("transaction_index").gt(0));
        if (start_time > 0) //default = 0
        {
            searchSourceBuilder.query(QueryBuilders.rangeQuery("timestamp").lt(start_time));
        }
        //page size
        searchSourceBuilder.size(count);
        //begin offset
        setSearchBuildFrom(page, count, searchSourceBuilder);

        searchSourceBuilder.sort("timestamp", SortOrder.DESC);
        searchRequest.source(searchSourceBuilder);
        searchSourceBuilder.trackTotalHits(true);
        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        return ServiceUtils.getSearchResult(searchResponse, TransactionWithEvent.class);
    }

    public Result<TransactionWithEvent> getNFTTxns(String network, long start_time, String address, int page, int count) throws IOException {
        String queryAddress = address.toLowerCase();
        Result<Event> events = getNFTEventByAddress(network, queryAddress, page, count);
        if (events.getTotal() < 1) {
            return Result.EmptyResult;
        }
        SearchRequest searchRequest = new SearchRequest(getIndex(network, Constant.TRANSACTION_INDEX));
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        BoolQueryBuilder executeBoolQuery = QueryBuilders.boolQuery();
        searchSourceBuilder.query(QueryBuilders.rangeQuery("transaction_index").gt(0));
        if (start_time > 0) {
            searchSourceBuilder.query(QueryBuilders.rangeQuery("timestamp").lt(start_time));
        }
        //page size
        searchSourceBuilder.size(count);
        //set offset
        setSearchBuildFrom(page, count, searchSourceBuilder);

        List<String> termHashes = new ArrayList<>();
        for (Event event : events.getContents()) {
            termHashes.add(event.getTransactionHash());
        }
        executeBoolQuery.should(QueryBuilders.termsQuery("transaction_hash", termHashes));
        searchSourceBuilder.query(executeBoolQuery);
        searchSourceBuilder.sort("timestamp", SortOrder.DESC);
        searchRequest.source(searchSourceBuilder);
        searchSourceBuilder.trackTotalHits(true);
        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        return ServiceUtils.getSearchResult(searchResponse, TransactionWithEvent.class);
    }

    public Result<PendingTransaction> getRangePendingTransaction(String network, int page, int count, int start_height) throws IOException {
        SearchRequest searchRequest = new SearchRequest(getIndex(network, Constant.PENDING_TXN_INDEX));
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.matchAllQuery());

        //page size
        searchSourceBuilder.size(count);
        //set offset
        setBlockSearchBuildFrom(page, count, start_height, searchSourceBuilder);

        searchSourceBuilder.sort("timestamp", SortOrder.DESC);
        searchRequest.source(searchSourceBuilder);
        searchSourceBuilder.trackTotalHits(true);
        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        return ServiceUtils.getSearchResult(searchResponse, PendingTransaction.class);
    }

    static void setBlockSearchBuildFrom(int page, int count, int start_height, SearchSourceBuilder searchSourceBuilder) {
        int offset = 0;
        boolean shouldAfter = false;
        if (page > 1) {
            offset = (page - 1) * count;
            if (offset >= ELASTICSEARCH_MAX_HITS && start_height > 0) {
                offset = start_height - (page - 1) * count;
                shouldAfter = true;
            }
        }
        if(shouldAfter) {
            searchSourceBuilder.searchAfter(new Object[]{offset});
        }else {
            searchSourceBuilder.from(offset);
        }
    }

    static void setSearchBuildFrom(int page, int count, SearchSourceBuilder searchSourceBuilder) {
        int offset = 0;
        boolean shouldAfter = false;
        if (page > 1) {
            offset = (page - 1) * count;
            if (offset >= ELASTICSEARCH_MAX_HITS) {
                shouldAfter = true;
            }
        }
        if(shouldAfter) {
            searchSourceBuilder.searchAfter(new Object[]{offset});
        }else {
            searchSourceBuilder.from(offset);
        }
    }

    public PendingTransaction getPending(String network, String id) throws IOException {
        GetRequest getRequest = new GetRequest(getIndex(network, Constant.PENDING_TXN_INDEX), id);
        GetResponse getResponse = client.get(getRequest, RequestOptions.DEFAULT);
        if (getResponse.isExists()) {
            String sourceAsString = getResponse.getSourceAsString();
            return JSON.parseObject(sourceAsString, PendingTransaction.class);
        } else {
            logger.error("not found transaction, id: {}", id);
            return null;
        }
    }

    public Result getRangeTransfers(String network, String typeTag, String receiver, String sender, int page, int count) {
        SearchRequest searchRequest = new SearchRequest(getIndex(network, Constant.TRANSFER_INDEX));
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        //page size
        searchSourceBuilder.size(count);
        //set offset
        setSearchBuildFrom(page, count, searchSourceBuilder);

        searchSourceBuilder.sort("timestamp", SortOrder.DESC);
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        if (typeTag != null && typeTag.length() > 0) {
            boolQueryBuilder.must(QueryBuilders.matchQuery("type_tag", typeTag));
        }
        if (receiver != null && receiver.length() > 0) {
            boolQueryBuilder.must(QueryBuilders.matchQuery("receiver", receiver));
        }
        if (sender != null && sender.length() > 0) {
            boolQueryBuilder.must(QueryBuilders.matchQuery("sender", sender));
        }
        searchSourceBuilder.query(boolQueryBuilder);
        searchRequest.source(searchSourceBuilder);
        searchSourceBuilder.trackTotalHits(true);
        SearchResponse searchResponse;
        try {
            searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            logger.error("get transfer error:", e);
            return Result.EmptyResult;
        }
        return ServiceUtils.getSearchResult(searchResponse, Transfer.class);
    }

    public Result getTransferCount(String network, String typeTag) {
        SearchRequest searchRequest = new SearchRequest(getIndex(network, Constant.TRANSFER_INDEX));
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.size(0);
        searchSourceBuilder.from(0);
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        if (typeTag != null && typeTag.length() > 0) {
            boolQueryBuilder.must(QueryBuilders.matchQuery("type_tag", typeTag));
        }
        searchSourceBuilder.aggregation(AggregationBuilders.count("token_count").field("type_tag.keyword"));
        searchSourceBuilder.query(boolQueryBuilder);
        searchRequest.source(searchSourceBuilder);

        searchSourceBuilder.trackTotalHits(true);
        SearchResponse searchResponse;
        TokenTransfer tokenTransfer = new TokenTransfer();
        Result<TokenTransfer> result = new Result<>();
        try {
            searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
            Map<String, Aggregation> aggMap = searchResponse.getAggregations().asMap();
            Aggregation aggregation = aggMap.get("token_count");
            if (aggregation instanceof ParsedValueCount) {
                tokenTransfer.setTransfers(((ParsedValueCount) aggregation).getValue());
                tokenTransfer.setTypeTag(typeTag);
                result.setContents(Collections.singletonList(tokenTransfer));
            }
        } catch (IOException e) {
            logger.error("get transfer error:", e);
            return Result.EmptyResult;
        }
        return result;
    }

    public Result<TransactionWithEvent> getRangeByAddress(String network, String address, int page, int count) throws IOException {
        SearchRequest searchRequest = new SearchRequest(getIndex(network, Constant.TRANSACTION_INDEX));
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.size(count);
        //begin offset
        int offset = 0;
        if (page > 1) {
            offset = (page - 1) * count;
        }
        searchSourceBuilder.from(offset);

        TermQueryBuilder termQueryBuilder = QueryBuilders.termQuery("user_transaction.raw_txn.sender", address);

        searchSourceBuilder.query(termQueryBuilder);
        searchRequest.source(searchSourceBuilder);
        searchSourceBuilder.trackTotalHits(true);
        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        return ServiceUtils.getSearchResult(searchResponse, TransactionWithEvent.class);
    }

    public Result<Event> getProposalEvents(String network, String eventAddress) throws IOException {
        SearchRequest searchRequest = new SearchRequest(getIndex(network, Constant.TRANSACTION_EVENT_INDEX));
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.size(ELASTICSEARCH_MAX_HITS);
        searchSourceBuilder.from(0);
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        boolQuery.filter(QueryBuilders.matchQuery("tag_name", ServiceUtils.proposalCreatedEvent));
        boolQuery.must(QueryBuilders.rangeQuery("transaction_index").gt(0));

        searchSourceBuilder.query(boolQuery);
        searchRequest.source(searchSourceBuilder);
        searchSourceBuilder.trackTotalHits(true);
        searchSourceBuilder.sort("timestamp", SortOrder.DESC);

        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        Result<Event> events = getSearchUnescapeResult(searchResponse, Event.class);
        List<Event> proposalEvents = new ArrayList<>();
        byte[] addressBytes = ByteUtils.hexToByteArray(eventAddress);
        AccountAddress proposer = null;
        try {
            proposer = AccountAddress.bcsDeserialize(addressBytes);
        } catch (DeserializationError deserializationError) {
            deserializationError.printStackTrace();
        }
        for (Event event : events.getContents()) {
            byte[] proposalBytes = ByteUtils.hexToByteArray(event.getData());
            try {
                ProposalCreatedEvent payload = ProposalCreatedEvent.bcsDeserialize(proposalBytes);

                if (payload.proposer.equals(proposer)) {
                    proposalEvents.add(event);
                }
            } catch (DeserializationError deserializationError) {
                deserializationError.printStackTrace();
            }
        }
        events.setContents(proposalEvents);
        events.setTotal(proposalEvents.size());
        return events;
    }

    public Result<Event> getNFTEventByAddress(String network, String address, int page, int count) throws IOException {
        SearchRequest searchRequest = new SearchRequest(getIndex(network, Constant.TRANSACTION_EVENT_INDEX));
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.size(count);
        //begin offset
        int offset = 0;
        if (page > 1) {
            offset = (page - 1) * count;
        }
        searchSourceBuilder.from(offset);

        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        boolQuery.must(QueryBuilders.rangeQuery("transaction_index").gt(0));
        if (address != null && address.length() > 0) {
            boolQuery.must(QueryBuilders.termQuery("event_address", address));
        }
        boolQuery.must(QueryBuilders.matchQuery("type_tag", "NFT").fuzziness("AUTO"));

        searchSourceBuilder.query(boolQuery);
        searchRequest.source(searchSourceBuilder);
        searchSourceBuilder.trackTotalHits(true);
        searchSourceBuilder.sort("timestamp", SortOrder.DESC);

        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        return getSearchUnescapeResult(searchResponse, Event.class);
    }

    public Result<Event> getEventsByAddress(String network, String address, int page, int count) throws IOException {
        SearchRequest searchRequest = new SearchRequest(getIndex(network, Constant.TRANSACTION_EVENT_INDEX));
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.size(count);
        //set offset
        setSearchBuildFrom(page, count, searchSourceBuilder);

        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        boolQuery.should(QueryBuilders.termQuery("tag_name", ServiceUtils.depositEvent));
        boolQuery.should(QueryBuilders.termQuery("tag_name", ServiceUtils.withdrawEvent));
        boolQuery.must(QueryBuilders.termQuery("event_address", address));
        boolQuery.must(QueryBuilders.rangeQuery("transaction_index").gt(0));

        searchSourceBuilder.query(boolQuery);
        searchRequest.source(searchSourceBuilder);
        searchSourceBuilder.trackTotalHits(true);
        searchSourceBuilder.sort("timestamp", SortOrder.DESC);

        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        return getSearchUnescapeResult(searchResponse, Event.class);
    }

    public Result getRangeByAddressAll(String network, String address, int page, int count, int txnQueryType, boolean withEvent) throws IOException {
        String queryAddress = address.toLowerCase();
        Result<Event> events = getEventsByAddress(network, queryAddress, page, count);
        Result<Event> proposalEvents = getProposalEvents(network, queryAddress);
        long total = events.getTotal() + proposalEvents.getTotal();
        if (total == 0) {
            return Result.EmptyResult;
        }
        SearchRequest searchRequest = new SearchRequest(getIndex(network, Constant.TRANSACTION_INDEX));
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.size(count);

        BoolQueryBuilder executeBoolQuery = QueryBuilders.boolQuery();
        HashSet<String> termHashes = new HashSet<>();
        getTxnHashes(events, termHashes);
        getTxnHashes(proposalEvents, termHashes);
        executeBoolQuery.should(QueryBuilders.termsQuery("transaction_hash", termHashes));
        if (txnQueryType > TransactionQueryType.ALL.getValue()) {
            TransactionType types = TransactionQueryType.fromValue(txnQueryType).toTransactionType();
            executeBoolQuery.must(QueryBuilders.termQuery("transaction_type.keyword", types.getName()));
        }

        searchSourceBuilder.query(executeBoolQuery);
        searchSourceBuilder.sort("timestamp", SortOrder.DESC);
        searchRequest.source(searchSourceBuilder);
        searchSourceBuilder.trackTotalHits(true);

        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        Result<TransactionWithEvent> result = ServiceUtils.getSearchResult(searchResponse, TransactionWithEvent.class);
        List<TransactionWithEvent> tempTxnContent =  result.getContents();
        if(withEvent && tempTxnContent.size() > 0) {
            //get events of query txn
            Result<Event> eventList = getEventsByTransaction(network, Lists.newArrayList(termHashes));
            Map<String, List<Event>> tempTxnEvents = new HashMap<>();
            for(Event event : eventList.getContents()) {
                String hash = event.getTransactionHash();
                List<Event> list = tempTxnEvents.get(hash);
                if(list == null || list.size() == 0) {
                    list = new ArrayList<>();
                }
                list.add(event);
                tempTxnEvents.put(hash, list);
            }
           //set event to txn object
           for(TransactionWithEvent txn : tempTxnContent) {
               String hash = txn.getTransactionHash();
               List<Event> list = tempTxnEvents.get(hash);
               if(list != null && list.size() > 0) {
                   txn.setEvents(list);
               }
           }
        }

        result.setTotal(total);
        return result;
    }

    private void getTxnHashes(Result<Event> proposalEvents, HashSet<String> termHashes) {
        for (Event event : proposalEvents.getContents()) {
            termHashes.add(event.getTransactionHash());
        }
    }

    public Result<TransactionWithEvent> getByBlockHash(String network, String blockHash) throws IOException {
        SearchRequest searchRequest = new SearchRequest(getIndex(network, Constant.TRANSACTION_INDEX));
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        TermQueryBuilder termQueryBuilder = QueryBuilders.termQuery("block_hash", blockHash);

        searchSourceBuilder.query(termQueryBuilder);
        searchSourceBuilder.sort("transaction_index", SortOrder.DESC);
        searchRequest.source(searchSourceBuilder);
        searchSourceBuilder.trackTotalHits(true);
        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        return ServiceUtils.getSearchResult(searchResponse, TransactionWithEvent.class);
    }

    public Result<TransactionWithEvent> getByBlockHeight(String network, int blockHeight) throws IOException {
        SearchRequest searchRequest = new SearchRequest(getIndex(network, Constant.TRANSACTION_INDEX));
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        TermQueryBuilder termQueryBuilder = QueryBuilders.termQuery("block_number", blockHeight);
        searchSourceBuilder.query(termQueryBuilder);
        searchRequest.source(searchSourceBuilder);
        searchSourceBuilder.trackTotalHits(true);
        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        Result<TransactionWithEvent> result = ServiceUtils.getSearchResult(searchResponse, TransactionWithEvent.class);
        //get events
        List<TransactionWithEvent> transactions = result.getContents();
        List<String> txnHashes = new ArrayList<>();
        Map<String, List<Event>> txnEvents = new HashMap<>();
        for (TransactionWithEvent txn : transactions) {
            txnHashes.add(txn.getTransactionHash());
            txnEvents.put(txn.getTransactionHash(), new ArrayList<>());
        }
        if (txnHashes.size() > 0) {
            Result<Event> events = getEventsByTransaction(network, txnHashes);
            for (Event event : events.getContents()) {
                txnEvents.get(event.getTransactionHash()).add(event);
            }
            //set events
            for (TransactionWithEvent txn : transactions) {
                txn.setEvents(txnEvents.get(txn.getTransactionHash()));
            }
        }
        return result;
    }

    public Result<Event> getEventsByTransaction(String network, List<String> txnHashes) throws IOException {
        SearchRequest searchRequest = new SearchRequest(getIndex(network, Constant.TRANSACTION_EVENT_INDEX));
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.size(ELASTICSEARCH_MAX_HITS);
        //begin offset
        int offset = 0;
        searchSourceBuilder.from(0);

        BoolQueryBuilder exersiceBoolQuery = QueryBuilders.boolQuery();
        exersiceBoolQuery.should(QueryBuilders.termsQuery("transaction_hash", txnHashes));
        searchSourceBuilder.query(exersiceBoolQuery);
        searchRequest.source(searchSourceBuilder);
        searchSourceBuilder.trackTotalHits(true);
        searchSourceBuilder.sort("timestamp", SortOrder.DESC);

        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        return getSearchUnescapeResult(searchResponse, Event.class);
    }


    public Result<Event> getEvents(String network, String tag_name, int page, int count) throws IOException {
        SearchRequest searchRequest = new SearchRequest(getIndex(network, Constant.TRANSACTION_EVENT_INDEX));
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.size(count);
        //begin offset
        int offset = 0;
        if (page > 1) {
            offset = (page - 1) * count;
        }
        searchSourceBuilder.from(offset);

        TermQueryBuilder termQueryBuilder = QueryBuilders.termQuery("tag_name.keyword", tag_name);
        searchSourceBuilder.query(termQueryBuilder);
        searchRequest.source(searchSourceBuilder);
        searchSourceBuilder.trackTotalHits(true);
        searchSourceBuilder.sort("timestamp", SortOrder.DESC);

        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        return getSearchUnescapeResult(searchResponse, Event.class);
    }
}
