package org.starcoin.search.handler;

import com.alibaba.fastjson.JSON;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;

import java.util.ArrayList;
import java.util.List;

public class ServiceUtils {

    public static final String depositEvent = "0x00000000000000000000000000000001::Account::DepositEvent";
    public static final String withdrawEvent = "0x00000000000000000000000000000001::Account::WithdrawEvent";
    public static final int ELASTICSEARCH_MAX_HITS = 10000;

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
}
