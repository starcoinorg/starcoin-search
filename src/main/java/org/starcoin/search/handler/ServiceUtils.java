package org.starcoin.search.handler;

import com.alibaba.fastjson.JSON;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.search.SearchHit;
import org.starcoin.api.Result;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ServiceUtils {

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

    static void createIndexIfNotExist(RestHighLevelClient client, String network, String index) throws IOException {
        String currentIndex = getIndex(network, index);
        GetIndexRequest getRequest = new GetIndexRequest(currentIndex);
        if (!client.indices().exists(getRequest, RequestOptions.DEFAULT)) {
            CreateIndexResponse response = client.indices().create(new CreateIndexRequest(currentIndex), RequestOptions.DEFAULT);
        }
    }
}
