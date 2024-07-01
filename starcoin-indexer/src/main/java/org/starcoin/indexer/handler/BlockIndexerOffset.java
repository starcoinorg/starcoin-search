package org.starcoin.indexer.handler;

import com.thetransactioncompany.jsonrpc2.client.JSONRPC2SessionException;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.GetMappingsRequest;
import org.elasticsearch.client.indices.GetMappingsResponse;
import org.elasticsearch.client.indices.PutMappingRequest;
import org.elasticsearch.cluster.metadata.MappingMetadata;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.starcoin.api.BlockRPCClient;
import org.starcoin.bean.Block;
import org.starcoin.bean.BlockHeader;
import org.starcoin.bean.BlockOffset;

import java.util.LinkedHashMap;
import java.util.Map;

public class BlockIndexerOffset {

    private static final Logger logger = LoggerFactory.getLogger(BlockIndexerOffset.class);

    private String offsetIndexer;

    private BlockOffset localBlockOffset;

    private BlockHeader currentHandleHeader;

    private RestHighLevelClient esClient;

    private BlockRPCClient blockRPCClient;


    public BlockIndexerOffset(String offsetIndexer, BlockRPCClient blockRPCClient, RestHighLevelClient client) {
        this.offsetIndexer = offsetIndexer;
        this.esClient = client;
        this.blockRPCClient = blockRPCClient;
    }

    public void initRemoteOffset() {
        //update current handle header
        try {
            localBlockOffset = getRemoteOffset();
            if (localBlockOffset != null) {
                Block block = blockRPCClient.getBlockByHeight(localBlockOffset.getBlockHeight());
                if (block != null) {
                    currentHandleHeader = block.getHeader();
                } else {
                    logger.error("init offset block not exist on chain: {}", localBlockOffset);
                }
            } else {
                logger.warn("offset is null,init reset to genesis");
                currentHandleHeader = blockRPCClient.getBlockByHeight(0).getHeader();
                updateBlockOffset(0L, currentHandleHeader.getBlockHash());
                logger.info("init offset ok: {}", localBlockOffset);
            }
        } catch (JSONRPC2SessionException e) {
            logger.error("set current header error:", e);
        }
    }

    public void updateBlockOffset(Long blockHeight, String blockHash) {
        if (localBlockOffset == null) {
            localBlockOffset = new BlockOffset(blockHeight, blockHash);
        } else {
            localBlockOffset.setBlockHeight(blockHeight);
            localBlockOffset.setBlockHash(blockHash);
        }
        setRemoteOffset(localBlockOffset);
    }

    public Long getLocalBlockOffsetHeight() {
        return localBlockOffset.getBlockHeight();
    }

    public String getLocalOffsetBlockHash() {
        return localBlockOffset.getBlockHash();
    }

    public BlockOffset getRemoteOffset() {
        GetMappingsRequest request = new GetMappingsRequest();
        try {
            request.indices(offsetIndexer);
            GetMappingsResponse response = esClient.indices().getMapping(request, RequestOptions.DEFAULT);
            MappingMetadata data = response.mappings().get(offsetIndexer);
            Object meta = data.getSourceAsMap().get("_meta");
            if (meta != null) {
                Map<String, Object> tip = (Map<String, Object>) ((LinkedHashMap<String, Object>) meta).get("tip");
                String blockHash = tip.get("block_hash").toString();
                Integer blockHeight = (Integer) tip.get("block_number");
                return new BlockOffset(blockHeight.longValue(), blockHash);
            }
        } catch (Exception e) {
            logger.error("get remote offset error:", e);
        }
        return null;
    }

    public void setRemoteOffset(BlockOffset blockOffset) {
        PutMappingRequest request = new PutMappingRequest(offsetIndexer);
        try {
            XContentBuilder builder = XContentFactory.jsonBuilder();
            builder.startObject();
            {
                builder.startObject("_meta");
                {
                    builder.startObject("tip");
                    {
                        builder.field("block_hash", blockOffset.getBlockHash());
                        builder.field("block_number", blockOffset.getBlockHeight());
                    }
                    builder.endObject();
                }
                builder.endObject();
            }
            builder.endObject();
            request.source(builder);
            esClient.indices().putMapping(request, RequestOptions.DEFAULT);
            logger.info("remote offset update ok : {}", blockOffset);
        } catch (Exception e) {
            logger.error("get remote offset error:", e);
        }
    }

    @Override
    public String toString() {
        return "BlockIndexerOffset{" +
                "offsetIndexer='" + offsetIndexer + '\'' +
                ", localBlockOffset=" + localBlockOffset +
                ", currentHandleHeader=" + currentHandleHeader +
                '}';
    }
}
