package org.starcoin.search.handler;

import com.novi.serde.DeserializationError;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
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
import org.starcoin.bean.EventFull;
import org.starcoin.search.constant.Constant;
import org.starcoin.types.event.DepositEvent;
import org.starcoin.utils.Hex;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class HolderHistoryHandle {
    private static Logger logger = LoggerFactory.getLogger(HolderHistoryHandle.class);
    @Autowired
    private StateRPCClient stateRPCClient;

    @Autowired
    private RestHighLevelClient client;
    @Value("${starcoin.network}")
    private String network;

    public void handle() {
        logger.info("holder handle begin...");
        Set<ElasticSearchHandler.AddressHolder> holders = new HashSet<>();
        //read event
        SearchRequest searchRequest = new SearchRequest(ServiceUtils.getIndex(network, Constant.EVENT_INDEX));
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.size(2000);

        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        boolQuery.must(QueryBuilders.termsQuery("tag_name.keyword", Constant.DEPOSIT_EVENT));
        boolQuery.must(QueryBuilders.rangeQuery("transaction_index").gt(0));

        searchSourceBuilder.query(boolQuery);
        searchSourceBuilder.sort("timestamp", SortOrder.ASC);
        searchRequest.source(searchSourceBuilder);
        searchRequest.scroll(TimeValue.timeValueMinutes(10));
        searchSourceBuilder.trackTotalHits(true);
        Result<EventFull> result = null;
        String scrollId = null;
        try {
            SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
            scrollId = searchResponse.getScrollId();
            result = ServiceUtils.getSearchResult(searchResponse, EventFull.class);
            addHolders(holders, result);
            while (true) {
                if (scrollId == null) {
                    break;
                }
                SearchScrollRequest searchScrollRequest = new SearchScrollRequest(scrollId).scroll(TimeValue.timeValueMinutes(10));
                searchResponse = client.scroll(searchScrollRequest, RequestOptions.DEFAULT);
                if (searchResponse != null && searchResponse.getHits().getHits().length > 0) {
                    result = ServiceUtils.getSearchResult(searchResponse, EventFull.class);
                    addHolders(holders, result);
                    logger.info("query: {}", scrollId);
                } else {
                    logger.info("query null");
                    break;
                }
            }
        } catch (IOException e) {
            logger.error("get event error:", e);
        }

        logger.info("holder size: {}", holders.size());
        BulkRequest bulkRequest = new BulkRequest();

        if (!holders.isEmpty()) {
            int batchSize = 500;
            int i = 0;
            for (ElasticSearchHandler.AddressHolder holder : holders
            ) {
                long amount = stateRPCClient.getAddressAmount(holder.getAddress(), holder.getTokenCode());
                logger.info("holder: {}, amount: {}", holder, amount);
                bulkRequest.add(buildHolderRequest(holder, amount));
                i += 1;
                if (i == batchSize) {
                    try {
                        BulkResponse response = client.bulk(bulkRequest, RequestOptions.DEFAULT);
                        logger.info("bulk block result: {}", response.buildFailureMessage());
                    } catch (IOException e) {
                        logger.error("bulk block error:", e);
                    }
                    bulkRequest = new BulkRequest();
                    i = 0;
                }
            }
            //the remain record handle
            try {
                BulkResponse response = client.bulk(bulkRequest, RequestOptions.DEFAULT);
                logger.info("bulk block result: {}", response.buildFailureMessage());
            } catch (IOException e) {
                logger.error("bulk block error:", e);
            }
        }
        logger.info("holder handle end...");
    }

    private void addHolders(Set<ElasticSearchHandler.AddressHolder> holders, Result<EventFull> result) {
        if (result == null) {
            return;
        }
        List<EventFull> eventFullList = result.getContents();
        for (EventFull event : eventFullList) {
            try {
                DepositEvent inner = DepositEvent.bcsDeserialize(Hex.decode(event.getData()));
                String sb = inner.token_code.address +
                        "::" +
                        inner.token_code.module +
                        "::" +
                        inner.token_code.name;
                holders.add(new ElasticSearchHandler.AddressHolder(event.getEventAddress(), sb));
            } catch (DeserializationError deserializationError) {
                logger.error("decode event data error:", deserializationError);
            }
        }
        logger.info("add ok: {}", holders.size());
    }

    private UpdateRequest buildHolderRequest(ElasticSearchHandler.AddressHolder holder, long amount) {
        String addressIndex = ServiceUtils.getIndex(network, Constant.ADDRESS_INDEX);
        try {
            String id = holder.getAddress() + "-" + holder.getTokenCode();
            XContentBuilder builder = XContentFactory.jsonBuilder();
            builder.startObject();
            {
                builder.field("address", holder.getAddress());
                builder.field("type_tag", holder.getTokenCode());
                builder.field("amount", amount);
            }
            builder.endObject();
            IndexRequest indexRequest = new IndexRequest(addressIndex);
            indexRequest.id(id).source(builder);
            UpdateRequest updateRequest = new UpdateRequest();
            updateRequest.index(addressIndex);
            updateRequest.id(id);
            updateRequest.doc(builder);
            updateRequest.upsert(indexRequest);
            return updateRequest;
        } catch (IOException e) {
            logger.error("build holder error:", e);
        }
        return null;
    }

}
