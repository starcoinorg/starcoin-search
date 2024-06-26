package org.starcoin.scan.service;

import com.alibaba.fastjson.JSONObject;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.TopHits;
import org.elasticsearch.search.aggregations.metrics.TopHitsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.starcoin.api.Result;
import org.starcoin.bean.DagInspectorBlock;
import org.starcoin.bean.DagInspectorEdge;
import org.starcoin.bean.DagInspectorHeightGroup;
import org.starcoin.constant.Constant;
import org.starcoin.scan.service.vo.*;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DagInspectorService extends BaseService {

    private static final Logger logger = LoggerFactory.getLogger(BlockService.class);

    @Autowired
    BlockService blockService;

    @Autowired
    private RestHighLevelClient client;


    public DIBlocksAndEdgesAndHeightGroupsVo getBlocksAndEdgesAndHeightGroups(String network, Long startHeight, Long endHeight) throws IOException {
        DIBlocksAndEdgesAndHeightGroupsVo groups = new DIBlocksAndEdgesAndHeightGroupsVo();

        List<DagInspectorBlock> blockList = getBlockList(network, startHeight, endHeight);
        if (blockList == null || blockList.isEmpty()) {
            return groups;
        }

        groups.setBlocks(blockList);
        groups.setEdges(getEdgeList(network, startHeight, endHeight));

        Set<Long> heightList =
                blockList.stream()
                        .map(DagInspectorBlock::getHeight)
                        .collect(Collectors.toSet());
        groups.setHeightGroups(getHeightGroup(network, heightList));

        return groups;
    }

    public DIBlocksAndEdgesAndHeightGroupsVo getBlockHash(
            String network,
            String targetHash,
            Long heightDifference
    ) throws IOException {
        DagInspectorBlock block = getBlockWithHashFromStorage(network, targetHash);
        if (block == null) {
            throw new RuntimeException("Cannot find block by block hash");
        }
        Long endHeight = block.getHeight();
        long startHeight = Math.max(block.getHeight() - heightDifference, 0L);
        return getBlocksAndEdgesAndHeightGroups(network, startHeight, endHeight);
    }

    public DIBlocksAndEdgesAndHeightGroupsVo getBlockDAAScore(
            String network,
            Long targetDAAScore,
            Long heightDifference
    ) throws IOException {
        DagInspectorBlock block = getHeightWithDAAScoreFromStorage(network, targetDAAScore);
        if (block == null) {
            throw new RuntimeException("Cannot find block by block hash");
        }
        Long endHeight = block.getHeight();
        long startHeight = Math.max(block.getHeight() - heightDifference, 0L);
        return getBlocksAndEdgesAndHeightGroups(network, startHeight, endHeight);
    }

    public DIBlocksAndEdgesAndHeightGroupsVo getHead(String network, Long heightDifference) throws IOException {
        long endHeight = getMaxHeightFromStorage(network);
        long startHeight = Math.max(endHeight - heightDifference, 0L);
        return getBlocksAndEdgesAndHeightGroups(network, startHeight, endHeight);
    }

    /**
     * Get block height from Elastic search storage
     *
     * @param network
     * @param startHeight
     * @param endHeight
     * @return
     */
    private List<DagInspectorBlock> getBlockList(String network, Long startHeight, Long endHeight) throws ElasticsearchException, IOException {
        SearchRequest searchRequest = new SearchRequest(getIndex(network, Constant.DAG_INSPECTOR_BLOCK));
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(QueryBuilders.boolQuery().must(QueryBuilders.rangeQuery("height").gte(startHeight).lte(endHeight)));
        sourceBuilder.sort("height", SortOrder.ASC);
        sourceBuilder.size(endHeight.intValue() - startHeight.intValue());
        searchRequest.source(sourceBuilder);
        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        Result<DagInspectorBlock> result = ServiceUtils.getSearchResult(searchResponse, DagInspectorBlock.class);
        return result.getContents();
    }

    /**
     * Get Edge list from ElasticSearch storage
     *
     * @param network
     * @param startHeight
     * @param endHeight
     * @return
     */
    List<DagInspectorEdge> getEdgeList(String network, Long startHeight, Long endHeight) {
        SearchRequest searchRequest = new SearchRequest(getIndex(network, Constant.DAG_INSPECTOR_EDGE));
        RangeQueryBuilder fromHeightQuery = QueryBuilders.rangeQuery("from_height").gte(startHeight);
        RangeQueryBuilder toHeightQuery = QueryBuilders.rangeQuery("to_height").lte(endHeight);
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder()
                .query(QueryBuilders.boolQuery().must(fromHeightQuery).must(toHeightQuery))
                .sort("to_height", SortOrder.ASC);
        searchRequest.source(sourceBuilder);
        SearchResponse searchResponse;
        try {
            searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            logger.error("getEdgeList failed, startHeight: {}, endHeight: {}", startHeight, endHeight, e);
            return null;
        }
        Result<DagInspectorEdge> result = ServiceUtils.getSearchResult(searchResponse, DagInspectorEdge.class);
        return result.getContents();
    }

    /**
     * Get heights
     *
     * @param network
     * @param heights
     * @return
     */
    protected List<DagInspectorHeightGroup> getHeightGroup(String network, Set<Long> heights) {
        SearchRequest searchRequest = new SearchRequest(getIndex(network, Constant.DAG_INSPECT_HEIGHT_GROUP));
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder()
                .query(QueryBuilders.termsQuery("height", heights));
        sourceBuilder.size(heights.size());
        searchRequest.source(sourceBuilder);
        SearchResponse searchResponse;
        try {
            searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            logger.error("getHeightGroup failed, heights: {}", heights, e);
            return null;
        }
        Result<DagInspectorHeightGroup> result = ServiceUtils.getSearchResult(searchResponse, DagInspectorHeightGroup.class);
        return result.getContents();
    }

    public DIAppConfigVo getAppConfig(String network) {
        return new DIAppConfigVo();
    }

    private Long getMaxHeightFromStorage(String network) throws IOException {
        SearchRequest searchRequest = new SearchRequest(getIndex(network, Constant.DAG_INSPECTOR_BLOCK));

        // Build the SearchSourceBuilder with max aggregation
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        TermsAggregationBuilder maxHeightAgg = AggregationBuilders
                .terms("max_height_agg")
                .field("height")
                .size(1);
        TopHitsAggregationBuilder topHitsAgg = AggregationBuilders
                .topHits("top_hits")
                .size(1);

        maxHeightAgg.subAggregation(topHitsAgg);
        searchSourceBuilder.aggregation(maxHeightAgg);
        searchRequest.source(searchSourceBuilder);

        // Execute the search request
        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        Terms maxHeightTerms = searchResponse.getAggregations().get("max_height_agg");
        if (maxHeightTerms.getBuckets().size() > 0) {
            Terms.Bucket bucket = maxHeightTerms.getBuckets().get(0);
            TopHits topHits = bucket.getAggregations().get("top_hits");
            if (topHits.getHits().getHits().length > 0) {
                String topHitsJson = topHits.getHits().getHits()[0].getSourceAsString();
                logger.info("Object with the max height: " + topHitsJson);
                DagInspectorBlock block = JSONObject.parseObject(topHitsJson, DagInspectorBlock.class);
                return block.getHeight();
            }
        }
        return 0L;
    }

    private DagInspectorBlock getBlockWithHashFromStorage(String network, String blockHash) {
        SearchRequest searchRequest = new SearchRequest(getIndex(network, Constant.DAG_INSPECTOR_BLOCK));
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        boolQueryBuilder.should(QueryBuilders.termQuery("block_hash", blockHash));

        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder().query(boolQueryBuilder);
        searchRequest.source(sourceBuilder);
        SearchResponse searchResponse;
        try {
            searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            logger.error("getBlockWithHashFromStorage failed, blockHash: {}", blockHash, e);
            return null;
        }
        Result<DagInspectorBlock> result = ServiceUtils.getSearchResult(searchResponse, DagInspectorBlock.class);
        return result.getContents().get(0);
    }

    private DagInspectorBlock getHeightWithDAAScoreFromStorage(String network, Long daaScore) {
        SearchRequest searchRequest = new SearchRequest(getIndex(network, Constant.DAG_INSPECTOR_BLOCK));
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        boolQueryBuilder.should(QueryBuilders.termQuery("daa_score", daaScore));

        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder().query(boolQueryBuilder);
        searchRequest.source(sourceBuilder);
        SearchResponse searchResponse;
        try {
            searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            logger.error("getBlockWithHashFromStorage failed, blockHash: {}", daaScore, e);
            return null;
        }
        Result<DagInspectorBlock> result = ServiceUtils.getSearchResult(searchResponse, DagInspectorBlock.class);
        return result.getContents().get(0);
    }

    public List<DagInspectorBlock> getBlocksByHeight(String network, Long height) {
        SearchRequest searchRequest = new SearchRequest(getIndex(network, Constant.DAG_INSPECTOR_BLOCK));
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        boolQueryBuilder.should(QueryBuilders.termQuery("height", height));

        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder().query(boolQueryBuilder);
        searchRequest.source(sourceBuilder);
        SearchResponse searchResponse;
        try {
            searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        } catch (IOException | org.elasticsearch.ElasticsearchStatusException e) {
            logger.error("getBlocksByHeight failed, blockHash: {}", height, e);
            return null;
        }
        Result<DagInspectorBlock> result = ServiceUtils.getSearchResult(searchResponse, DagInspectorBlock.class);
        return result.getContents();
    }

}
