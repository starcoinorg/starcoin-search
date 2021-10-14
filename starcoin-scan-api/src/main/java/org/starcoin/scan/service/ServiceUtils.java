package org.starcoin.scan.service;

import com.alibaba.fastjson.JSON;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.starcoin.api.Result;
import org.starcoin.utils.ByteUtils;

import java.util.ArrayList;
import java.util.List;

public class ServiceUtils {
    public static final String depositEvent = "DepositEvent";
    public static final String withdrawEvent = "WithdrawEvent";
    public static final String proposalCreatedEvent = "ProposalCreatedEvent";
    public static final int ELASTICSEARCH_MAX_HITS = 10000;

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

    public static <T> Result<T> getSearchUnescapeResult(SearchResponse searchResponse, Class<T> object) {
        SearchHit[] searchHit = searchResponse.getHits().getHits();
        Result<T> result = new Result<>();
        result.setTotal(searchResponse.getHits().getTotalHits().value);
        List<T> blocks = new ArrayList<>();
        for (SearchHit hit : searchHit) {
            blocks.add(JSON.parseObject(ByteUtils.unescapeEvent(hit.getSourceAsString()), object));
        }
        result.setContents(blocks);
        return result;
    }
}
