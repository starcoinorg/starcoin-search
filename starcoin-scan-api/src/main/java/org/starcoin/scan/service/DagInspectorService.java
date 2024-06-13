package org.starcoin.scan.service;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.starcoin.api.Result;
import org.starcoin.bean.Block;
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

    public DIBlocksAndEdgesAndHeightGroups getBlocksBetweenHeights(String network, Integer startHeight, Integer endHeight) {
        List<Block> blockList = blockService.getBlocksBetweenHeights(network, startHeight, endHeight);

        DIBlocksAndEdgesAndHeightGroups groups = new DIBlocksAndEdgesAndHeightGroups();
        groups.setBlocks(blockList.stream().map(block -> {
            DIBlock diBlock = new DIBlock();
            diBlock.setBlockHash(block.getHeader().getBlockHash());
            diBlock.setHeight(block.getHeader().getHeight());
            diBlock.setColor("blue");
            diBlock.setInVirtualSelectedParentChain(false);
            diBlock.setParentIds(block.getHeader().getParentsHash());

            // TODO(BobOng): Get selected parent id from chain data
            // diBlock.setSelectedParentHash();
            // diBlock.setHeightGroupIndex();
            // diBlock.setDaaScore();
            return diBlock;
        }).collect(Collectors.toList()));

        groups.setEdges(getEdgeList(network, startHeight, endHeight));

        List<Long> heights = blockList
                .stream()
                .map(block -> block.getHeader().getHeight())
                .collect(Collectors.toList());

        List<DIHeightGroup> diHeightGroup = getHeightGroup(network, heights);
        groups.setHeightGroups(diHeightGroup);

        return groups;
    }

    public DIBlocksAndEdgesAndHeightGroups getBlockHash(String targetHash, Integer heightDifference) {
        return new DIBlocksAndEdgesAndHeightGroups();
    }

    public DIBlocksAndEdgesAndHeightGroups getBlockDAAScore(Integer targetDAAScore, Integer heightDifference) {
        return new DIBlocksAndEdgesAndHeightGroups();
    }

    public DIBlocksAndEdgesAndHeightGroups getHead(Integer heightDifference) {
        return new DIBlocksAndEdgesAndHeightGroups();
    }

    List<DIEdge> getEdgeList(String network, Integer startHeight, Integer endHeight) {
        SearchRequest searchRequest = new SearchRequest(getIndex(network, Constant.DAG_INSPECTOR_EDGE));
        RangeQueryBuilder fromHeightQuery = QueryBuilders.rangeQuery("from_height").gte(startHeight);
        RangeQueryBuilder toHeightQuery = QueryBuilders.rangeQuery("to_height").lte(endHeight);
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder().query(QueryBuilders.boolQuery().must(fromHeightQuery).must(toHeightQuery)).sort("to_height", SortOrder.ASC);
        searchRequest.source(sourceBuilder);
        SearchResponse searchResponse;
        try {
            searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            logger.error("getEdgeList failed, startHeight: {}, endHeight: {}", startHeight, endHeight, e);
            return null;
        }
        Result<DIEdge> result = ServiceUtils.getSearchResult(searchResponse, DIEdge.class);
        return result.getContents();
    }

    List<DIHeightGroup> getHeightGroup(String network, List<Long> heights) {
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
        Result<DIHeightGroup> result = ServiceUtils.getSearchResult(searchResponse, DIHeightGroup.class);
        return result.getContents();
    }

    public DIAppConfig getAppConfig() {
        return new DIAppConfig();
    }

}
