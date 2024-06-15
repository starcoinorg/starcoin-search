package org.starcoin.indexer.handler;

import com.alibaba.fastjson.JSONObject;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.index.query.TermsQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.starcoin.api.BlockRPCClient;
import org.starcoin.api.Result;
import org.starcoin.bean.*;
import org.starcoin.constant.Constant;
import org.starcoin.jsonrpc.client.JSONRPC2SessionException;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DagInspectorHandler {

    private static final Logger logger = LoggerFactory.getLogger(DagInspectorHandler.class);

    private static final String NODE_COLOR_GRAY = "gray";
    private static final String NODE_COLOR_RED = "red";
    private static final String NODE_COLOR_BLUE = "blue";

    @Value("${starcoin.network}")
    private String network;

    String dagInspectNodeIndex;
    String dagInspectEdgeIndex;
    String dagInspectHeightGroupIndex;

    @Autowired
    private RestHighLevelClient client;

    @Autowired
    private BlockRPCClient blockRPCClient;

    @PostConstruct
    public void initIndexs() throws IOException {
        dagInspectNodeIndex = ServiceUtils.createIndexIfNotExist(client, network, Constant.DAG_INSPECTOR_NODE);
        dagInspectEdgeIndex = ServiceUtils.createIndexIfNotExist(client, network, Constant.DAG_INSPECTOR_EDGE);
        dagInspectHeightGroupIndex = ServiceUtils.createIndexIfNotExist(client, network, Constant.DAG_INSPECT_HEIGHT_GROUP);
    }

    public void upsertDagInfoFromBlocks(List<Block> blockList) throws IOException {
        if (blockList.isEmpty()) {
            logger.warn("block list is empty");
            return;
        }
        BulkRequest bulkRequest = new BulkRequest();

        // Handle blockGhostdagDataCache
        Map<String, DagInspectorBlock> inspBlockMap = bulkLoadInspectorBlock(
                blockList.stream()
                        .map(block -> block.getHeader().getBlockHash())
                        .collect(Collectors.toList())
        );

        List<DagInspectorHeightGroup> inspHeightGroupList = getGroupHeightSizeFromStorage(
                blockList.stream()
                        .map(block -> block.getHeader().getHeight())
                        .distinct()
                        .collect(Collectors.toList())
        );

        // Build block node data
        blockList.forEach(blockInfo -> {
            String currentBlockHash = blockInfo.getHeader().getBlockHash();
            DagInspectorBlock inspecBlock = inspBlockMap.get(currentBlockHash);
            if (inspecBlock == null) {
                try {
                    BlockGhostdagData ghostdagData = blockRPCClient.getBlockGhostdagData(currentBlockHash);
                    inspecBlock = new DagInspectorBlock();
                    inspecBlock.setBlockHash(currentBlockHash);
                    inspecBlock.setTimestamp(blockInfo.getHeader().getTimestamp());
                    inspecBlock.setColor(NODE_COLOR_GRAY);
                    inspecBlock.setDaaScore(ghostdagData.getBlueScore());
                    inspecBlock.setHeight(blockInfo.getHeader().getHeight());
                    inspecBlock.setSelectedParentHash(ghostdagData.getSelectedParent());
                    inspecBlock.setParentIds(blockInfo.getHeader().getParentsHash());

                    Integer groupSize = getHeightGroupSizeOrDefault(inspHeightGroupList, inspecBlock.getHeight(), 0);
                    inspecBlock.setHeightGroupIndex(groupSize);
                    updateGroupSize(inspHeightGroupList, inspecBlock.getHeight(), groupSize + 1);

                    inspBlockMap.put(currentBlockHash, inspecBlock);

                } catch (JSONRPC2SessionException e) {
                    throw new RuntimeException(e);
                }
            } else {
                logger.info("Block {} already exists", currentBlockHash);
            }
        });


        // Save all data into storage
        List<DagInspectorBlock> inspectorBlockList =  new ArrayList<>(inspBlockMap.values());
        bulkRequest.add(buildSaveInspectorBlockRequest(inspectorBlockList));

        List<DagInspectorEdge> edgeList = buildEdgeDataFromNodeData(inspectorBlockList);
        bulkRequest.add(buildSaveEdgeRequest(edgeList));

        bulkRequest.add(buildSaveHeightGroupRequest(inspHeightGroupList));

        // Bulk save
        try {
            BulkResponse response = client.bulk(bulkRequest, RequestOptions.DEFAULT);
            logger.info("bulk block result: {}", response.buildFailureMessage());
        } catch (IOException e) {
            logger.error("bulk block error:", e);
        }
    }

    List<DagInspectorEdge> buildEdgeDataFromNodeData(List<DagInspectorBlock> inspecBlockList) {
        List<DagInspectorEdge> edgeList = new ArrayList<>();
        if (inspecBlockList.isEmpty()) {
            return edgeList;
        }

        for (DagInspectorBlock block : inspecBlockList) {
            if (block.getParentIds().isEmpty()) {
                continue;
            }

            block.getParentIds().forEach(parentHash -> {
                DagInspectorBlock parentBlock = inspecBlockList
                        .stream()
                        .filter(b -> b.getBlockHash().equalsIgnoreCase(parentHash))
                        .findFirst()
                        .orElse(null);
                if (parentBlock == null) {
                    logger.info("Parent block {} not found", parentHash);
                    return;
                }
                DagInspectorEdge edge = new DagInspectorEdge();
                edge.setFromBlockHash(block.getBlockHash());
                edge.setToBlockHash(parentBlock.getBlockHash());
                edge.setFromHeight(block.getHeight());
                edge.setToHeight(parentBlock.getHeight());
                edge.setFromHeightGroupIndex(block.getHeightGroupIndex());
                edge.setToHeightGroupIndex(parentBlock.getHeightGroupIndex());
                edgeList.add(edge);
            });
        }
        return edgeList;
    }

    public Map<String, DagInspectorBlock> bulkLoadInspectorBlock(List<String> blockHashList) {
        Map<String, DagInspectorBlock> inspBlockMap = new HashMap<>();
        SearchRequest searchRequest = new SearchRequest(dagInspectNodeIndex);
        TermsQueryBuilder termQueryBuilder = QueryBuilders.termsQuery("block_hash", blockHashList);
        searchRequest.source(new SearchSourceBuilder().query(termQueryBuilder));
        try {
            SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);
            JSONObject obj = JSONObject.parseObject(ServiceUtils.getJsonString(response));
            List<JSONObject> hits = obj.getJSONObject("hits").getJSONArray("hits").toJavaList(JSONObject.class);
            hits.forEach(hit -> {
                JSONObject source = hit.getJSONObject("_source");
                DagInspectorBlock block = new DagInspectorBlock();
                block.setBlockHash(source.getString("block_hash"));
                block.setTimestamp(source.getLong("timestamp"));
                block.setColor(source.getString("color"));
                block.setDaaScore(source.getLong("daa_score"));
                block.setHeight(source.getLong("height"));
                block.setHeightGroupIndex(source.getInteger("height_group_index"));
                block.setSelectedParentHash(source.getString("selected_parent_hash"));
                block.setParentIds(source.getJSONArray("parent_ids").toJavaList(String.class));
                block.setInVirtualSelectedParentChain(source.getBoolean("in_virtual_selected_parent_chain"));
                block.setMergeSetRedIds(source.getJSONArray("mergeset_red_ids").toJavaList(String.class));
                block.setMergeSetBlueIds(source.getJSONArray("mergeset_blue_ids").toJavaList(String.class));
                inspBlockMap.put(block.getBlockHash(), block);
            });
        } catch (IOException e) {
            logger.error("bulkLoadInspectorBlock error:", e);
        }
        return inspBlockMap;
    }

    IndexRequest buildSaveInspectorBlockRequest(List<DagInspectorBlock> blockList) {
        IndexRequest request = new IndexRequest(dagInspectNodeIndex);
        if (blockList.isEmpty()) {
            return request;
        }
        blockList.forEach(block -> {
            XContentBuilder builder;
            try {
                builder = XContentFactory.jsonBuilder();
                builder.startObject();
                {
                    builder.field("block_hash", block.getBlockHash());
                    builder.field("height", block.getHeight());
                    builder.field("timestamp", block.getTimestamp());
                    builder.field("selected_parent_hash", block.getSelectedParentHash());
                    builder.field("parent_ids", block.getParentIds());
                    builder.field("daa_score", block.getDaaScore());
                    builder.field("height_group_index", block.getHeightGroupIndex());
                    builder.field("in_virtual_selected_parent_chain", block.getInVirtualSelectedParentChain());
                    builder.field("mergeset_blue_ids", block.getMergeSetBlueIds());
                    builder.field("mergeset_red_ids", block.getMergeSetRedIds());
                    builder.field("color", block.getColor());
                }
                builder.endObject();
            } catch (Exception e) {
                logger.error("build block error:", e);
            }
        });
        return request;
    }

    IndexRequest buildSaveEdgeRequest(List<DagInspectorEdge> edgeList) {
        IndexRequest request = new IndexRequest(dagInspectEdgeIndex);
        if (edgeList.isEmpty()) {
            return request;
        }
        edgeList.forEach(edge -> {
            XContentBuilder builder;
            try {
                builder = XContentFactory.jsonBuilder();
                builder.startObject();
                {
                    builder.field("from_block_hash", edge.getFromBlockHash());
                    builder.field("to_block_hash", edge.getToBlockHash());
                    builder.field("from_height", edge.getFromHeight());
                    builder.field("to_height", edge.getToHeight());
                    builder.field("from_group_index", edge.getFromHeightGroupIndex());
                    builder.field("to_group_index", edge.getToHeightGroupIndex());
                }
                builder.endObject();
            } catch (Exception e) {
                logger.error("build block error:", e);
            }
        });
        return request;
    }

    IndexRequest buildSaveHeightGroupRequest(List<DagInspectorHeightGroup> heightGroupList) {
        IndexRequest request = new IndexRequest(dagInspectHeightGroupIndex);
        if (heightGroupList.isEmpty()) {
            return request;
        }
        heightGroupList.forEach(heightGroup -> {
            XContentBuilder builder;
            try {
                builder = XContentFactory.jsonBuilder();
                builder.startObject();
                {
                    builder.field("height", heightGroup.getHeight());
                    builder.field("size", heightGroup.getSize());
                }
                builder.endObject();
            } catch (Exception e) {
                logger.error("build block error:", e);
            }
        });
        return request;
    }

    /**
     * Get the size of the group at the specified height, or return the default size if the group does not exist.
     * @param groupList
     * @param height
     * @param defaultSize
     * @return
     */
    Integer getHeightGroupSizeOrDefault(List<DagInspectorHeightGroup> groupList, Long height, Integer defaultSize) {
        if (groupList.isEmpty()) {
            return defaultSize;
        }
        DagInspectorHeightGroup group = groupList
                .stream()
                .filter(g -> g.getHeight().longValue() == height)
                .findFirst()
                .orElse(null);
        return (group != null) ? group.getSize() : defaultSize;
    }

    void updateGroupSize(List<DagInspectorHeightGroup> groupList, Long height, Integer newSize) {
        for (DagInspectorHeightGroup group : groupList) {
            if (group.getHeight().longValue() == height) {
                group.setSize(newSize);
                break;
            }
        }
    }

    private IndexRequest buildHeightGroup(Block block, Integer blockHeightIndex) {
        IndexRequest request = new IndexRequest(dagInspectHeightGroupIndex);
        XContentBuilder builder = null;
        try {
            builder = XContentFactory.jsonBuilder();
            builder.startObject();

            builder.field("height", block.getHeader().getHeight());
            builder.field("size", blockHeightIndex + 1);

            builder.endObject();

        } catch (Exception e) {
            logger.error("build block error:", e);
        }
        request.source(builder);
        return request;
    }


    private List<DagInspectorHeightGroup> getGroupHeightSizeFromStorage(List<Long> heights) throws IOException {
        List<DagInspectorHeightGroup> groupList = new ArrayList<>();
        if (heights.isEmpty()) {
            return groupList;
        }

        SearchRequest searchRequest = new SearchRequest(dagInspectHeightGroupIndex);
        TermsQueryBuilder termQueryBuilder = QueryBuilders.termsQuery("height", heights);
        searchRequest.source(new SearchSourceBuilder().query(termQueryBuilder));
        SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);
        Result<DagInspectorHeightGroup> result = ServiceUtils.getSearchResult(response, DagInspectorHeightGroup.class);
        return result.getContents();
    }

}
