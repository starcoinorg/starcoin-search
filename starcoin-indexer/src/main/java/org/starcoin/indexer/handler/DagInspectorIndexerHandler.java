package org.starcoin.indexer.handler;

import com.alibaba.fastjson.JSONArray;
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
import org.elasticsearch.index.query.TermsQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.starcoin.api.BlockRPCClient;
import org.starcoin.api.Result;
import org.starcoin.bean.*;
import org.starcoin.constant.Constant;
import org.starcoin.jsonrpc.client.JSONRPC2SessionException;
import org.starcoin.utils.ExceptionWrap;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DagInspectorIndexerHandler {

    private static final Logger logger = LoggerFactory.getLogger(DagInspectorIndexerHandler.class);

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
        dagInspectNodeIndex = ServiceUtils.createIndexIfNotExist(client, network, Constant.DAG_INSPECTOR_BLOCK);
        dagInspectEdgeIndex = ServiceUtils.createIndexIfNotExist(client, network, Constant.DAG_INSPECTOR_EDGE);
        dagInspectHeightGroupIndex = ServiceUtils.createIndexIfNotExist(client, network, Constant.DAG_INSPECT_HEIGHT_GROUP);
    }

    public void upsertDagInfoFromBlocks(List<Block> blockList) throws IOException, JSONRPC2SessionException {
        if (blockList.isEmpty()) {
            logger.warn("block list is empty");
            return;
        }
        BulkRequest bulkRequest = new BulkRequest();

        // Handle blockGhostdag Data Cache
        Map<String, DagInspectorBlock> dagBlockMap = bulkLoadDagBlock(
                blockList.stream()
                        .map(block -> block.getHeader().getBlockHash())
                        .collect(Collectors.toList())
        );

        List<DagInspectorHeightGroup> heightGroupList = getGroupHeightSizeFromStorage(
                blockList.stream()
                        .map(block -> block.getHeader().getHeight())
                        .distinct()
                        .collect(Collectors.toList())
        );

        // Build block node data
        blockList.forEach(blockInfo -> {
            String currentBlockHash = blockInfo.getHeader().getBlockHash();
            DagInspectorBlock dagBlock = dagBlockMap.get(currentBlockHash);
            if (dagBlock == null) {
                try {
                    BlockGhostdagData ghostdagData = blockRPCClient.getBlockGhostdagData(currentBlockHash);
                    dagBlock = new DagInspectorBlock();
                    dagBlock.setBlockHash(currentBlockHash);
                    dagBlock.setTimestamp(blockInfo.getHeader().getTimestamp());
                    dagBlock.setColor(NODE_COLOR_GRAY);
                    dagBlock.setDaaScore(ghostdagData.getBlueScore());
                    dagBlock.setHeight(blockInfo.getHeader().getHeight());
                    dagBlock.setSelectedParentHash(ghostdagData.getSelectedParent());
                    dagBlock.setParentIds(blockInfo.getHeader().getParentsHash());
                    // Block is the virtual selected parent chain because the list read from block height
                    dagBlock.setInVirtualSelectedParentChain(true);

                    Integer groupSize = getHeightGroupSizeOrDefault(heightGroupList, dagBlock.getHeight(), 0);
                    dagBlock.setHeightGroupIndex(groupSize);
                    updateGroupSize(heightGroupList, dagBlock.getHeight(), groupSize + 1);

                    dagBlockMap.put(currentBlockHash, dagBlock);

                } catch (JSONRPC2SessionException e) {
                    throw new RuntimeException(e);
                }
            } else {
                logger.info("Block {} already exists", currentBlockHash);
            }
        });

        buildSaveDagEdgeRequest(buildEdgeDataFromDagBlockDataMaybeUpate(dagBlockMap, heightGroupList)).forEach(bulkRequest::add);
        buildSaveDagHeightGroupRequest(heightGroupList).forEach(bulkRequest::add);

        // Save all data into storage
        buildSaveDagBlockRequest(new ArrayList<>(dagBlockMap.values())).forEach(bulkRequest::add);

        // Bulk save
        try {
            BulkResponse response = client.bulk(bulkRequest, RequestOptions.DEFAULT);
            logger.info("bulk block result: {}", response.buildFailureMessage());
        } catch (IOException e) {
            logger.error("bulk block error:", e);
        }
    }

    List<DagInspectorEdge> buildEdgeDataFromDagBlockDataMaybeUpate(
            Map<String, DagInspectorBlock> dagBlockMap,
            List<DagInspectorHeightGroup> heightGroupList
    ) throws JSONRPC2SessionException {
        List<DagInspectorEdge> edgeList = new ArrayList<>();
        if (dagBlockMap.isEmpty()) {
            return edgeList;
        }

        List<DagInspectorBlock> newDagBlocks = new ArrayList<>();
        for (DagInspectorBlock dagBlock : dagBlockMap.values()) {
            if (dagBlock.getParentIds().isEmpty()) {
                continue;
            }

            for (String parentHash : dagBlock.getParentIds()) {
                DagInspectorBlock parentDagBlock = dagBlockMap.get(parentHash);
                if (parentDagBlock == null) {
                    logger.info("Parent block not found: {} ", parentHash);
                    parentDagBlock = getDagInspectorBlockInfoFromHash(parentHash, heightGroupList, false);

                    // Put into buffer list
                    newDagBlocks.add(parentDagBlock);
                }
                DagInspectorEdge edge = new DagInspectorEdge();
                edge.setFromBlockHash(dagBlock.getBlockHash());
                edge.setToBlockHash(parentDagBlock.getBlockHash());
                edge.setFromHeight(dagBlock.getHeight());
                edge.setToHeight(parentDagBlock.getHeight());
                edge.setFromHeightGroupIndex(dagBlock.getHeightGroupIndex() == null ? 0 : dagBlock.getHeightGroupIndex());
                edge.setToHeightGroupIndex(parentDagBlock.getHeightGroupIndex() == null ? 0 :parentDagBlock.getHeightGroupIndex());
                edgeList.add(edge);
            }
        }
        // Put into map
        newDagBlocks.forEach(dagBlock -> dagBlockMap.put(dagBlock.getBlockHash(), dagBlock));
        return edgeList;
    }

    public Map<String, DagInspectorBlock> bulkLoadDagBlock(List<String> blockHashList) {
        Map<String, DagInspectorBlock> dagBlockMap = new HashMap<>();
        SearchRequest searchRequest = new SearchRequest(dagInspectNodeIndex);
        TermsQueryBuilder termQueryBuilder = QueryBuilders.termsQuery("block_hash", blockHashList);
        searchRequest.source(new SearchSourceBuilder().query(termQueryBuilder));
        try {
            SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);
            JSONObject obj = JSONObject.parseObject(ServiceUtils.getJsonString(response));
            List<JSONObject> hits = obj.getJSONObject("hits").getJSONArray("hits").toJavaList(JSONObject.class);
            hits.forEach(hit -> {
                JSONObject source = hit.getJSONObject("sourceAsMap");
                DagInspectorBlock block = new DagInspectorBlock();
                block.setBlockHash(source.getString("block_hash"));
                block.setTimestamp(source.getLong("timestamp"));
                block.setColor(source.getString("color"));
                block.setDaaScore(source.getLong("daa_score"));
                block.setHeight(source.getLong("height"));
                block.setHeightGroupIndex(source.getInteger("height_group_index"));
                block.setSelectedParentHash(source.getString("selected_parent_hash"));

                JSONArray parentIds = source.getJSONArray("parent_ids");
                block.setParentIds(parentIds != null ? parentIds.toJavaList(String.class) : new ArrayList<>());

                Boolean virtual_selected = source.getBoolean("in_virtual_selected_parent_chain");
                block.setInVirtualSelectedParentChain(virtual_selected != null ? virtual_selected : false);

                JSONArray redIds = source.getJSONArray("mergeset_red_ids");
                block.setMergeSetRedIds(redIds != null ? redIds.toJavaList(String.class) : new ArrayList<>());

                JSONArray blueIds = source.getJSONArray("mergeset_blue_ids");
                block.setMergeSetBlueIds(blueIds != null ? blueIds.toJavaList(String.class) : new ArrayList<>());

                dagBlockMap.put(block.getBlockHash(), block);
            });
        } catch (IOException e) {
            logger.error("bulkLoadInspectorBlock error:", e);
        }
        return dagBlockMap;
    }

    List<IndexRequest> buildSaveDagBlockRequest(List<DagInspectorBlock> blockList) {
        if (blockList.isEmpty()) {
            return new ArrayList<>();
        }
        return blockList.stream().map(ExceptionWrap.wrap(block -> {
            IndexRequest request = new IndexRequest(dagInspectNodeIndex);
            XContentBuilder builder = XContentFactory.jsonBuilder();
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
            request.source(builder).id(block.getBlockHash());
            return request;
        })).collect(Collectors.toList());
    }

    private List<IndexRequest> buildSaveDagEdgeRequest(List<DagInspectorEdge> edgeList) throws IOException {
        if (edgeList.isEmpty()) {
            return new ArrayList<>();
        }
        return edgeList.stream().map(ExceptionWrap.wrap(edge -> {
            IndexRequest request = new IndexRequest(dagInspectEdgeIndex);
            XContentBuilder builder = XContentFactory.jsonBuilder();
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
            request.source(builder).id(String.format("%s_%s", edge.getFromBlockHash(), edge.getToBlockHash()));
            return request;
        })).collect(Collectors.toList());
    }

    List<IndexRequest> buildSaveDagHeightGroupRequest(List<DagInspectorHeightGroup> heightGroupList) {
        if (heightGroupList.isEmpty()) {
            return new ArrayList<>();
        }
        return heightGroupList.stream().map(ExceptionWrap.wrap(dagInspectorHeightGroup -> {
            IndexRequest request = new IndexRequest(dagInspectHeightGroupIndex);

            XContentBuilder builder = XContentFactory.jsonBuilder();
            builder.startObject();
            builder.field("height", dagInspectorHeightGroup.getHeight());
            builder.field("size", dagInspectorHeightGroup.getSize());
            builder.endObject();
            request.id(String.valueOf(dagInspectorHeightGroup.getHeight())).source(builder);
            return request;
        })).collect(Collectors.toList());
    }

    /**
     * Get the size of the group at the specified height, or return the default size if the group does not exist.
     *
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
                return;
            }
        }
        DagInspectorHeightGroup group = new DagInspectorHeightGroup();
        group.setHeight(height);
        group.setSize(newSize);
        groupList.add(group);
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


    protected DagInspectorBlock getDagInspectorBlockInfoFromHash(
            String blockHash,
            List<DagInspectorHeightGroup> heightGroupList,
            boolean isSelectedParentChain
    ) throws JSONRPC2SessionException {

        Block blockInfo = blockRPCClient.getBlockByHash(blockHash);
        BlockGhostdagData blockGhostdagData = blockRPCClient.getBlockGhostdagData(blockHash);
        DagInspectorBlock dagBlock = new DagInspectorBlock();

        Long blockHeight = blockInfo.getHeader().getHeight();

        dagBlock.setBlockHash(blockHash);
        dagBlock.setTimestamp(blockInfo.getHeader().getTimestamp());
        dagBlock.setColor(NODE_COLOR_GRAY);
        dagBlock.setDaaScore(blockGhostdagData.getBlueScore());
        dagBlock.setHeight(blockInfo.getHeader().getHeight());
        dagBlock.setSelectedParentHash(blockGhostdagData.getSelectedParent());
        dagBlock.setParentIds(blockInfo.getHeader().getParentsHash());
        dagBlock.setInVirtualSelectedParentChain(isSelectedParentChain);

        // Height group list index
        Integer groupSize = getHeightGroupSizeOrDefault(heightGroupList, blockHeight, 0);
        dagBlock.setHeightGroupIndex(groupSize);
        updateGroupSize(heightGroupList, blockHeight, groupSize + 1);

        logger.info("Get block info from hash: {}, height: {}, heightGroupIndex: {}",
                blockHash,
                blockInfo.getHeader().getHeight(),
                dagBlock.getHeightGroupIndex()
        );
        return dagBlock;
    }
}
