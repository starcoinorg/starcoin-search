package org.starcoin.scan.service;

import com.alibaba.fastjson.JSON;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.starcoin.api.Result;
import org.starcoin.bean.Block;
import org.starcoin.bean.UncleBlock;
import org.starcoin.constant.Constant;

import java.io.IOException;
import java.util.List;

import static org.starcoin.scan.service.ServiceUtils.ELASTICSEARCH_MAX_HITS;

@Service
public class BlockService extends BaseService {
    private static final Logger logger = LoggerFactory.getLogger(BlockService.class);

    @Autowired
    private RestHighLevelClient client;

    public Block getBlock(String network, String id) throws IOException {
        GetRequest getRequest = new GetRequest(getIndex(network, Constant.BLOCK_CONTENT_INDEX), id);
        GetResponse getResponse = client.get(getRequest, RequestOptions.DEFAULT);
        Block block = new Block();
        if (getResponse.isExists()) {
            String sourceAsString = getResponse.getSourceAsString();
            block = JSON.parseObject(sourceAsString, Block.class);
        } else {
            logger.error("not found block by id: {}", id);
        }
        return block;
    }

    public Block getBlockByHash(String network, String hash) {
        SearchRequest searchRequest = new SearchRequest(getIndex(network, Constant.BLOCK_CONTENT_INDEX));
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        TermQueryBuilder termQueryBuilder = QueryBuilders.termQuery("header.block_hash", hash);
        searchSourceBuilder.query(termQueryBuilder);
        searchRequest.source(searchSourceBuilder);
        SearchResponse searchResponse;
        try {
            searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            logger.error("get block by hash error:", e);
            return null;
        }
        Result<Block> result = ServiceUtils.getSearchResult(searchResponse, Block.class);
        List<Block> blocks = result.getContents();
        if (blocks.size() == 1) {
            return blocks.get(0);
        } else {
            logger.warn("get block by hash is null, network: {}, : {}", network, hash);
        }
        return null;
    }

    public Block getBlockByHeight(String network, long height) {
        SearchRequest searchRequest = new SearchRequest(getIndex(network, Constant.BLOCK_IDS_INDEX));
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        TermQueryBuilder termQueryBuilder = QueryBuilders.termQuery("header.number", height);
        searchSourceBuilder.query(termQueryBuilder);
        searchRequest.source(searchSourceBuilder);
        SearchResponse searchResponse;
        try {
            searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            logger.error("get block by height error:", e);
            return null;
        }
        Result<Block> result = ServiceUtils.getSearchResult(searchResponse, Block.class);
        List<Block> blocks = result.getContents();
        if (blocks.size() == 1) {
            return getBlockByHash(network, blocks.get(0).getHeader().getBlockHash());
        } else {
            logger.warn("get block by height is null, network: {}, : {}", network, height);
        }
        return null;
    }

    public Result<Block> getRange(String network, int page, int count, int start_height) {
        SearchRequest searchRequest = new SearchRequest(getIndex(network, Constant.BLOCK_IDS_INDEX));
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.matchAllQuery());
        //page size
        searchSourceBuilder.size(count);
        //set offset
        TransactionService.setBlockSearchBuildFrom(page, count, start_height, searchSourceBuilder);

        searchSourceBuilder.sort("header.number", SortOrder.DESC);
        searchSourceBuilder.trackTotalHits(true);
        searchRequest.source(searchSourceBuilder);
        SearchResponse searchResponse;
        try {
            searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            logger.error("get range block error:", e);
            return null;
        }
        return ServiceUtils.getSearchResult(searchResponse, Block.class);
    }

    public Result<Block> getBlocksStartWith(String network, long start_height, int page, int count) {
        SearchRequest searchRequest = new SearchRequest(getIndex(network, Constant.BLOCK_IDS_INDEX));
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        if (start_height == 0) {
            searchSourceBuilder.query(QueryBuilders.matchAllQuery());
        } else {
            searchSourceBuilder.query(QueryBuilders.rangeQuery("header.number").lt(start_height));
        }

        //page size
        searchSourceBuilder.size(count);
        //begin offset
        int offset;
        if (page > 1) {
            offset = (page - 1) * count;
            if (offset >= ELASTICSEARCH_MAX_HITS) {
                searchSourceBuilder.searchAfter(new Object[]{offset});
            } else {
                searchSourceBuilder.from(offset);
            }
        }
        searchSourceBuilder.sort("header.number", SortOrder.DESC);
        searchSourceBuilder.trackTotalHits(true);
        searchRequest.source(searchSourceBuilder);
        SearchResponse searchResponse;
        try {
            searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            logger.error("get range block error:", e);
            return null;
        }
        return ServiceUtils.getSearchResult(searchResponse, Block.class);
    }

    public UncleBlock getUncleBlockByHeight(String network, long height) {
        SearchRequest searchRequest = new SearchRequest(getIndex(network, Constant.UNCLE_BLOCK_INDEX));
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        TermQueryBuilder termQueryBuilder = QueryBuilders.termQuery("header.number", height);
        searchSourceBuilder.query(termQueryBuilder);
        searchRequest.source(searchSourceBuilder);
        SearchResponse searchResponse;
        try {
            searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            logger.error("get uncle block error:", e);
            return null;
        }
        Result<UncleBlock> result = ServiceUtils.getSearchResult(searchResponse, UncleBlock.class);
        List<UncleBlock> blocks = result.getContents();
        if (blocks.size() == 1) {
            return blocks.get(0);
        } else {
            logger.warn("get uncle block by height is null, network: {}, : {}", network, height);
        }
        return null;
    }

    public UncleBlock getUncleBlockByHash(String network, String hash) {
        SearchRequest searchRequest = new SearchRequest(getIndex(network, Constant.UNCLE_BLOCK_INDEX));
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        TermQueryBuilder termQueryBuilder = QueryBuilders.termQuery("header.block_hash", hash);
        searchSourceBuilder.query(termQueryBuilder);
        searchRequest.source(searchSourceBuilder);
        SearchResponse searchResponse;
        try {
            searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            logger.error("get uncle block by hash error:", e);
            return null;
        }
        Result<UncleBlock> result = ServiceUtils.getSearchResult(searchResponse, UncleBlock.class);
        List<UncleBlock> blocks = result.getContents();
        if (blocks.size() == 1) {
            return blocks.get(0);
        } else {
            logger.warn("get uncle block by hash is null, network: {}, : {}", network, hash);
        }
        return null;
    }

    public Result<UncleBlock> getUnclesRange(String network, int page, int count, int start_height) {
        SearchRequest searchRequest = new SearchRequest(getIndex(network, Constant.UNCLE_BLOCK_INDEX));
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.matchAllQuery());
        //page size
        searchSourceBuilder.size(count);
        //set offset
        TransactionService.setBlockSearchBuildFrom(page, count, start_height, searchSourceBuilder);

        searchSourceBuilder.sort("uncle_block_number", SortOrder.DESC);
        searchSourceBuilder.trackTotalHits(true);
        searchRequest.source(searchSourceBuilder);
        SearchResponse searchResponse;
        try {
            searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            logger.error("get uncle range error:", e);
            return null;
        }
        return ServiceUtils.getSearchResult(searchResponse, UncleBlock.class);
    }


}
