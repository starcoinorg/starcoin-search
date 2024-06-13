package org.starcoin.indexer.handler;

import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.starcoin.bean.*;
import org.starcoin.constant.Constant;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DagInspectorHandler {

    private static final Logger logger = LoggerFactory.getLogger(DagInspectorHandler.class);

    @Value("${starcoin.network}")
    private String network;

    String dagInspectNodeIndex;
    String dagInspectEdgeIndex;
    String dagInspectHeightGroupIndex;

    @Autowired
    private RestHighLevelClient client;

    @PostConstruct
    public void initIndexs() throws IOException {
        dagInspectNodeIndex = ServiceUtils.createIndexIfNotExist(client, network, Constant.DAG_INSPECTOR_NODE);
        dagInspectEdgeIndex = ServiceUtils.createIndexIfNotExist(client, network, Constant.DAG_INSPECTOR_EDGE);
        dagInspectHeightGroupIndex = ServiceUtils.createIndexIfNotExist(client, network, Constant.DAG_INSPECT_HEIGHT_GROUP);
    }

    public void upsertDagInfoFromBlocks(List<Block> blockList) {
        if (blockList.isEmpty()) {
            logger.warn("block list is empty");
            return;
        }
        BulkRequest bulkRequest = new BulkRequest();

        blockList.forEach(block -> {
            bulkRequest.add(buildNodeRequest(block));
            bulkRequest.add(buildEdgeRequest(block, blockList));
            bulkRequest.add(buildHeightGroupRequest(block));
        });

        try {
            BulkResponse response = client.bulk(bulkRequest, RequestOptions.DEFAULT);
            logger.info("bulk block result: {}", response.buildFailureMessage());
        } catch (IOException e) {
            logger.error("bulk block error:", e);
        }
    }

    /***
     * Build node index request, include block hash, parents hash, colors, etc.
     * @param block
     * @return IndexRequest
     */
    private IndexRequest buildNodeRequest(Block block) {
        IndexRequest request = new IndexRequest(dagInspectNodeIndex);
        XContentBuilder builder;
        try {
            builder = XContentFactory.jsonBuilder();
            builder.startObject();
            {
                builder.field("block_hash", block.getHeader().getBlockHash());
                builder.field("parents_hash", block.getHeader().getParentsHash());
                // TODO(BobOng): get colors from nodes
            }
            builder.endObject();
        } catch (Exception e) {
            logger.error("build block error:", e);
            return null;
        }
        request.id(String.valueOf(block.getHeader().getBlockHash())).source(builder);
        return request;
    }

    /**
     * Build edge data into elasticsearch, include from_block_id, to_block_id, from_height, to_height, etc.
     *
     * @param block block from chain data
     * @return IndexRequest
     */
    private IndexRequest buildEdgeRequest(Block block, List<Block> blockList) {
        IndexRequest request = new IndexRequest(dagInspectEdgeIndex);
        XContentBuilder builder;
        try {
            builder = XContentFactory.jsonBuilder();
            builder.startObject();
            {
                buildEdgeFieldFromBlock(builder, block, blockList);
            }
            builder.endObject();
        } catch (Exception e) {
            logger.error("build block error:", e);
            return null;
        }
        request.id(String.valueOf(block.getHeader().getBlockHash())).source(builder);
        return request;
    }

    private void buildEdgeFieldFromBlockPair(XContentBuilder builder, Block block, Block parentBlock) throws IOException {
        builder.startObject();

        builder.field("from_block_id", block.getHeader().getBlockHash());
        builder.field("to_block_id", parentBlock.getHeader().getBlockHash());
        builder.field("from_height", block.getHeader().getHeight());
        builder.field("to_height", parentBlock.getHeader().getHeight());
        builder.field("to_height_group_index", 0);

        builder.endObject();
    }

    /**
     * Build edge field from block, include from_block_id, to_block_id, from_height, to_height, etc.
     *
     * @param builder
     * @param currentBlock
     * @param blockList
     * @throws IOException
     */
    private void buildEdgeFieldFromBlock(XContentBuilder builder, Block currentBlock, List<Block> blockList) throws IOException {
        BlockHeader currentBlockHeader = currentBlock.getHeader();

        String parentHash = currentBlockHeader.getParentHash();
        Block parentBlock = blockList
                .stream()
                .filter(b -> b.getHeader().getBlockHash().equalsIgnoreCase(parentHash))
                .findFirst()
                .orElse(null);
        if (parentBlock == null) {
            // TODO(BobOng): Get parent block from chain data
            logger.info("Need to getting the parent block from chain data, parent hash: {}", parentHash);
        }
        buildEdgeFieldFromBlockPair(builder, currentBlock, parentBlock);

        if (currentBlockHeader.getParentsHash().isEmpty()) {
            return;
        }

        List<String> filteredParents = currentBlockHeader
                .getParentsHash()
                .stream()
                .filter(h -> h.equalsIgnoreCase(parentHash))
                .collect(Collectors.toList());

        if (filteredParents.isEmpty()) {
            return;
        }

        List<Block> parentBlocks = blockList
                .stream()
                .filter(b -> filteredParents.contains(b.getHeader().getBlockHash()))
                .collect(Collectors.toList());
        for (Block parentBLock : parentBlocks) {
            buildEdgeFieldFromBlockPair(builder, currentBlock, parentBLock);
        }
    }

    private IndexRequest buildHeightGroupRequest(Block block) {
        IndexRequest request = new IndexRequest(dagInspectHeightGroupIndex);
        XContentBuilder builder = null;
        try {
            builder = XContentFactory.jsonBuilder();
            builder.startObject();
            builder.field("height", block.getHeader().getHeight());

//            builder.field("from_block_id", block.getHeader().getBlockHash());
//            builder.field("to_block_id", parentBlock.getHeader().getBlockHash());
//            builder.field("from_height", block.getHeader().getHeight());
//            builder.field("to_height", parentBlock.getHeader().getHeight());
//            builder.field("to_height_group_index", 0);
            builder.endObject();

        } catch (Exception e) {
            logger.error("build block error:", e);
        }
        request.source(builder);
        return request;
    }


}
