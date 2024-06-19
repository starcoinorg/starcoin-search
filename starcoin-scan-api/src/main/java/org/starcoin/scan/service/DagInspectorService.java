package org.starcoin.scan.service;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.metrics.Max;
import org.elasticsearch.search.builder.SearchSourceBuilder;
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
import java.util.stream.Collectors;

@Service
public class DagInspectorService extends BaseService {

    private static final Logger logger = LoggerFactory.getLogger(BlockService.class);

    @Autowired
    BlockService blockService;

    @Autowired
    private RestHighLevelClient client;


    public DIBlocksAndEdgesAndHeightGroupsVo getBlocksAndEdgesAndHeightGroups(String network, Long startHeight, Long endHeight) {
        DIBlocksAndEdgesAndHeightGroupsVo groups = new DIBlocksAndEdgesAndHeightGroupsVo();

        List<DagInspectorBlock> blockList = getBlockList(network, startHeight, endHeight);
        if (blockList == null || blockList.isEmpty()) {
            return groups;
        }

        groups.setBlocks(blockList);
        groups.setEdges(getEdgeList(network, startHeight, endHeight));

        List<Long> heightList =
                blockList.stream()
                        .map(DagInspectorBlock::getHeight)
                        .collect(Collectors.toList());
        groups.setHeightGroups(getHeightGroup(network, heightList));

        return groups;
    }

    public DIBlocksAndEdgesAndHeightGroupsVo getBlockHash(String network, String targetHash, Integer heightDifference) {
        DagInspectorBlock block = getBlockWithHashFromStorage(network, targetHash);
        if (block == null) {
            throw new RuntimeException("Cannot find block by block hash");
        }
        Long endHeight = block.getHeight();
        long startHeight = block.getHeight() - heightDifference;
        if (startHeight < 0L) {
            startHeight = 0L;
        }
        return getBlocksAndEdgesAndHeightGroups(network, startHeight, endHeight);
    }

    public DIBlocksAndEdgesAndHeightGroupsVo getBlockDAAScore(String network, Integer targetDAAScore, Integer heightDifference) {
        DagInspectorBlock block = getHeightWithDAAScoreFromStorage(network, targetDAAScore);
        if (block == null) {
            throw new RuntimeException("Cannot find block by block hash");
        }
        Long endHeight = block.getHeight();
        long startHeight = block.getHeight() - heightDifference;
        if (startHeight < 0L) {
            startHeight = 0L;
        }
        return getBlocksAndEdgesAndHeightGroups(network, startHeight, endHeight);
    }

    public DIBlocksAndEdgesAndHeightGroupsVo getHead(String network, Long heightDifference) {
        long endHeight = getMaxHeightFromStorage();
        long startHeight = endHeight - heightDifference;
        if (startHeight < 0L) {
            startHeight = 0L;
        }
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
    private List<DagInspectorBlock> getBlockList(String network, Long startHeight, Long endHeight) {
        SearchRequest searchRequest = new SearchRequest(getIndex(network, Constant.DAG_INSPECTOR_NODE));
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(QueryBuilders.boolQuery().must(QueryBuilders.rangeQuery("height").gte(startHeight).lte(endHeight)));
        sourceBuilder .sort("height", SortOrder.ASC);
        searchRequest.source(sourceBuilder);
        SearchResponse searchResponse;
        try {
            searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            logger.error("getEdgeList failed, startHeight: {}, endHeight: {}", startHeight, endHeight, e);
            return null;
        }
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
    List<DagInspectorHeightGroup> getHeightGroup(String network, List<Long> heights) {
        SearchRequest searchRequest = new SearchRequest(getIndex(network, Constant.DAG_INSPECT_HEIGHT_GROUP));
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        for (Long height : heights) {
            boolQueryBuilder.should(QueryBuilders.termQuery("height", height));
        }
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder().query(boolQueryBuilder);
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

    public DIAppConfigVo getAppConfig() {
        return new DIAppConfigVo();
    }

    private Long getMaxHeightFromStorage() {
        final String MAX_HEIGHT_FIELD = "max_height";
        final String HEIGHT_FIELD = "height";

        try {
            SearchRequest searchRequest = new SearchRequest(Constant.DAG_INSPECTOR_NODE);

            // Build the SearchSourceBuilder with max aggregation
            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
            sourceBuilder.aggregation(AggregationBuilders.max(MAX_HEIGHT_FIELD).field(HEIGHT_FIELD));
            searchRequest.source(sourceBuilder);

            // Execute the search request
            SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);

            // Handle the response
            Max maxHeight = searchResponse.getAggregations().get("max_height");
            return (long) maxHeight.getValue();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0L;
    }

    private DagInspectorBlock getBlockWithHashFromStorage(String network, String blockHash) {
        SearchRequest searchRequest = new SearchRequest(getIndex(network, Constant.DAG_INSPECTOR_NODE));
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

    private DagInspectorBlock getHeightWithDAAScoreFromStorage(String network, Integer daaScore) {
        SearchRequest searchRequest = new SearchRequest(getIndex(network, Constant.DAG_INSPECTOR_NODE));
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

}
