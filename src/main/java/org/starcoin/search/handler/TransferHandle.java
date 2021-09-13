package org.starcoin.search.handler;

import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.starcoin.api.Result;
import org.starcoin.bean.Transfer;
import org.starcoin.search.bean.TransferOffset;
import org.starcoin.search.constant.Constant;
import org.starcoin.search.utils.ResultWithId;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.math.BigInteger;
import java.util.List;

import static org.starcoin.search.constant.Constant.TRANSFER_INDEX;

@Service
public class TransferHandle {

    private static final Logger logger = LoggerFactory.getLogger(TransferHandle.class);
    private final RestHighLevelClient client;
    private String offsetIndex;
    @Value("${starcoin.network}")
    private String network;

    public TransferHandle(RestHighLevelClient client) {
        this.client = client;
    }

    @PostConstruct
    public void initIndexes() {
        logger.info("init transfer indices...");
        try {
            ServiceUtils.createIndexIfNotExist(client, network, Constant.TRANSFER_INDEX);
            ServiceUtils.createIndexIfNotExist(client, network, Constant.ADDRESS_INDEX);
            ServiceUtils.createIndexIfNotExist(client, network, Constant.TRANSFER_JOURNAL_INDEX);
            offsetIndex = ServiceUtils.getIndex(network, TRANSFER_INDEX);
            logger.info(" transfer index init ok!");
        } catch (IOException e) {
            logger.error("init index error:", e);
        }
    }

    public TransferOffset getRemoteOffset() {
        return ServiceUtils.getRemoteOffset(client, offsetIndex);
    }

    public void setRemoteOffset(TransferOffset offset) {
        ServiceUtils.setRemoteOffset(client, offsetIndex, offset);
    }

    public Result<Transfer> getRangeTransfers(TransferOffset transferOffset, int count) {
        SearchRequest searchRequest = new SearchRequest(ServiceUtils.getIndex(network, Constant.TRANSFER_INDEX));
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        //page size
        searchSourceBuilder.size(count);
        searchSourceBuilder.from(0);
        searchSourceBuilder.sort("timestamp", SortOrder.ASC);
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        boolQueryBuilder.must(QueryBuilders.rangeQuery("timestamp").gt(transferOffset.getTimestamp()));

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
        //set ids
        ResultWithId<Transfer> result = ServiceUtils.getSearchResultWithIds(searchResponse, Transfer.class);
        List<Transfer> transferList = result.getContents();
        List<String> ids = result.getIds();
        if (!transferList.isEmpty()) {
            for (int i = 0; i < transferList.size(); i++) {
                transferList.get(i).setId(ids.get(i));
            }
        }
        return result;
    }

    public void bulk(List<Transfer> transferList, int offset) {
        if (transferList.isEmpty()) {
            logger.warn("transfer list is null");
            return;
        }
        int size = transferList.size();
        BulkRequest bulkRequest = new BulkRequest();
        long timestamp = 0;
        int successSize = offset;
        for (Transfer transfer : transferList) {
            // update token index
            BigInteger amount = transfer.getAmountValue();
            IndexRequest senderRequest = buildJournalRequest(transfer.getId(), transfer.getTypeTag(), transfer.getSender(), amount.negate(), transfer.getTimestamp());
            if (senderRequest != null) {
                bulkRequest.add(senderRequest);
            } else {
                break;
            }
            IndexRequest receiveRequest = buildJournalRequest(transfer.getId(), transfer.getTypeTag(), transfer.getReceiver(), amount, transfer.getTimestamp());
            if (senderRequest != null) {
                bulkRequest.add(receiveRequest);
            } else {
                break;
            }
            successSize += 1;
            timestamp = transfer.getTimestamp();
        }
        try {
            BulkResponse response = client.bulk(bulkRequest, RequestOptions.DEFAULT);
            logger.info("bulk transfer result: {}", response.buildFailureMessage());
            //update offset
            TransferOffset transferOffset = new TransferOffset();
            transferOffset.setOffset(successSize);
            transferOffset.setTimestamp(String.valueOf(timestamp));
            this.setRemoteOffset(transferOffset);
        } catch (IOException e) {
            logger.error("bulk transfer error:", e);
        }
    }

    private IndexRequest buildJournalRequest(String transferId, String typeTag, String address, BigInteger amount, long timestamp) {
        XContentBuilder addressBuilder = getJournalBuilder(transferId, typeTag, address, amount, timestamp);
        if (addressBuilder != null) {
            String addressIndex = ServiceUtils.getIndex(network, Constant.TRANSFER_JOURNAL_INDEX);
            IndexRequest indexRequest = new IndexRequest(addressIndex);
            indexRequest.source(addressBuilder);
            return indexRequest;
        }
        return null;
    }

    private XContentBuilder getJournalBuilder(String transferId, String typeTag, String address, BigInteger amount, long timestamp) {
        try {
            XContentBuilder builder = XContentFactory.jsonBuilder();
            builder.startObject();
            {
                builder.field("transfer_id", transferId);
                builder.field("type_tag", typeTag);
                builder.field("address", address);
                builder.field("amount", amount);
                builder.field("timestamp", timestamp);
            }
            builder.endObject();
            return builder;
        } catch (IOException e) {
            logger.error("token holder builder err:", e);
        }
        return null;
    }
}
