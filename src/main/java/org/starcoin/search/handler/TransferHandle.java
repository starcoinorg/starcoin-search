package org.starcoin.search.handler;

import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.GetMappingsRequest;
import org.elasticsearch.client.indices.GetMappingsResponse;
import org.elasticsearch.client.indices.PutMappingRequest;
import org.elasticsearch.cluster.metadata.MappingMetadata;
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

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.starcoin.search.constant.Constant.TRANSFER_INDEX;

@Service
public class TransferHandle {

    private static final Logger logger = LoggerFactory.getLogger(TransferHandle.class);
    private final RestHighLevelClient client;
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
            logger.info(" transfer index init ok!");
        } catch (IOException e) {
            logger.error("init index error:", e);
        }
    }

    public TransferOffset getRemoteOffset() {
        GetMappingsRequest request = new GetMappingsRequest();
        try {
            String offsetIndex = ServiceUtils.getIndex(network, TRANSFER_INDEX);
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
            logger.error("get transfer offset error:", e);
        }
        return null;
    }

    public void setRemoteOffset(TransferOffset offset) {
        String offsetIndex = ServiceUtils.getIndex(network, TRANSFER_INDEX);
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
        return ServiceUtils.getSearchResult(searchResponse, Transfer.class);
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
            long amount = transferAmount(transfer.getAmount());
            IndexRequest senderRequest = buildJournalRequest(transfer.getTypeTag(), transfer.getSender(), -amount, transfer.getTimestamp());
            if (senderRequest != null) {
                bulkRequest.add(senderRequest);
            } else {
                break;
            }
            IndexRequest receiveRequest = buildJournalRequest(transfer.getTypeTag(), transfer.getReceiver(), amount, transfer.getTimestamp());
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

    private long transferAmount(String amountStr) {
        if (amountStr == null && (!amountStr.startsWith("0x"))) {
            logger.warn("amount not right: {}", amountStr);
            return 0;
        }
        int len = amountStr.length();
        int index = 0;
        for (int i = len - 1; i > 1; i--) {
            if (!(amountStr.charAt(i) == '0')) {
                index = i;
                break;
            }
        }
        try {
            if (index + 1 > 2) {
                String tempStr = amountStr.substring(2, index + 1);
                return Long.parseLong(tempStr, 16);
            } else {
                logger.warn("amountStr too short: {}", amountStr);
            }
        } catch (NumberFormatException e) {
            logger.error("transfer amount error:", e);
        }
        return 0;
    }

    private IndexRequest buildJournalRequest(String typeTag, String address, long amount, long timestamp) {
        XContentBuilder addressBuilder = getJournalBuilder(typeTag, address, amount, timestamp);
        if (addressBuilder != null) {
            String addressIndex = ServiceUtils.getIndex(network, Constant.TRANSFER_JOURNAL_INDEX);
            IndexRequest indexRequest = new IndexRequest(addressIndex);
            indexRequest.source(addressBuilder);
            return indexRequest;
        }
        return null;
    }

    private XContentBuilder getJournalBuilder(String typeTag, String address, long amount, long timestamp) {
        try {
            XContentBuilder builder = XContentFactory.jsonBuilder();
            builder.startObject();
            {
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
