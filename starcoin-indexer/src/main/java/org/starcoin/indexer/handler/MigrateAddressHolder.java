package org.starcoin.indexer.handler;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.starcoin.api.Result;
import org.starcoin.bean.AddressHolderEntity;
import org.starcoin.constant.Constant;
import org.starcoin.indexer.service.AddressHolderService;

import java.io.IOException;
import java.util.Date;
import java.util.List;

@Service
public class MigrateAddressHolder {
    private static Logger logger = LoggerFactory.getLogger(MigrateAddressHolder.class);
    private static int COUNT = 0;
    @Autowired
    private RestHighLevelClient client;
    @Autowired
    private AddressHolderService addressHolderService;
    @Value("${starcoin.network}")
    private String network;

    public void handle() {
        logger.info("holder migrate begin...");
        //read event
        SearchRequest searchRequest = new SearchRequest(ServiceUtils.getIndex(network, Constant.ADDRESS_INDEX));
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.size(2000);

        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        boolQuery.must(QueryBuilders.matchAllQuery());

        searchSourceBuilder.query(boolQuery);
        searchRequest.source(searchSourceBuilder);
        searchRequest.scroll(TimeValue.timeValueMinutes(10));
        searchSourceBuilder.trackTotalHits(true);
        Result<AddressHolderEntity> result = null;
        String scrollId = null;
        try {
            SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
            scrollId = searchResponse.getScrollId();
            result = ServiceUtils.getSearchResult(searchResponse, AddressHolderEntity.class);
            saveHolders(result);
            while (true) {
                if (scrollId == null) {
                    break;
                }
                SearchScrollRequest searchScrollRequest = new SearchScrollRequest(scrollId).scroll(TimeValue.timeValueMinutes(10));
                searchResponse = client.scroll(searchScrollRequest, RequestOptions.DEFAULT);
                if (searchResponse != null && searchResponse.getHits().getHits().length > 0) {
                    result = ServiceUtils.getSearchResult(searchResponse, AddressHolderEntity.class);
                    saveHolders(result);
                    logger.info("query: {}", scrollId);
                } else {
                    logger.info("query null");
                    break;
                }
            }
        } catch (IOException e) {
            logger.error("get holder error:", e);
        }

        logger.info("holder migrated size: {}", COUNT);
    }

    private void saveHolders(Result<AddressHolderEntity> result) {
        if(result == null) return;
        List<AddressHolderEntity> addressHolderList = result.getContents();
        for (AddressHolderEntity holder: addressHolderList) {
            holder.setUpdateTime(new Date());
            addressHolderService.save(holder);
            COUNT++;
        }
    }
}
