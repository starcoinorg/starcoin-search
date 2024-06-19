package org.starcoin.indexer.handler;

import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.starcoin.api.BlockRPCClient;
import org.starcoin.bean.Block;
import org.starcoin.bean.BlockHeader;
import org.starcoin.bean.BlockOffset;
import org.starcoin.jsonrpc.client.JSONRPC2SessionException;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class DagInspectorIndexer extends QuartzJobBean {

    private static final Logger logger = LoggerFactory.getLogger(DagInspectorIndexer.class);

    @Autowired
    private DagInspectorIndexerHandler inspectorHandler;

    @Autowired
    private ElasticSearchHandler elasticSearchHandler;

    @Autowired
    private BlockRPCClient blockRPCClient;

    @Value("${starcoin.indexer.bulk_size}")
    private long bulkSize;

    private BlockOffset localBlockOffset;

    private BlockHeader currentHandleHeader;

    @PostConstruct
    public void initOffset() {
        localBlockOffset = elasticSearchHandler.getRemoteOffset();
        //update current handle header
        try {
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
                localBlockOffset = new BlockOffset(0, currentHandleHeader.getBlockHash());
                elasticSearchHandler.setRemoteOffset(localBlockOffset);
                logger.info("init offset ok: {}", localBlockOffset);
            }
        } catch (JSONRPC2SessionException e) {
            logger.error("set current header error:", e);
        }
    }

    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) {
        //read current offset
        if (localBlockOffset == null || currentHandleHeader == null) {
            initOffset();
        }
        BlockOffset remoteBlockOffset = elasticSearchHandler.getRemoteOffset();
        logger.info("current remote offset: {}", remoteBlockOffset);
        if (remoteBlockOffset == null) {
            logger.warn("offset must not null, please check blocks.mapping!!");
            return;
        }

        if (remoteBlockOffset.getBlockHeight() > localBlockOffset.getBlockHeight()) {
            logger.info("indexer equalize chain blocks.");
            return;
        }

        //read head
        try {
            BlockHeader chainHeader = blockRPCClient.getChainHeader();
            // Calculate bulk size
            long headHeight = chainHeader.getHeight();
            long bulkNumber = Math.min(headHeight - localBlockOffset.getBlockHeight(), bulkSize);
            int index = 1;
            Map<String, Block> blockMap = new HashMap<>();
            while (index <= bulkNumber) {
                long readNumber = localBlockOffset.getBlockHeight() + index;

                logger.info("Start Get block number: {}", index);
                Block block = blockRPCClient.getBlockByHeight(readNumber);
                if (block == null) {
                    logger.warn("get block null: {}", readNumber);
                    return;
                }

                logger.info("Block number is: {}, block hash is: {}", readNumber, block.getHeader().getBlockHash());

                for (String parentHash : block.getHeader().getParentsHash()) {
                    logger.info(
                            "Start get block hash : {}, parent hash: {}",
                            block.getHeader().getBlockHash(),
                            parentHash
                    );

                    if (!blockMap.containsKey(parentHash)) {
                        Block parentBlock = blockRPCClient.getBlockByHash(parentHash);
                        if (parentBlock == null) {
                            logger.warn("get parent block null: {}", parentHash);
                            continue;
                        }
                        blockMap.put(parentHash, parentBlock);
                    }
                }
                //update current header
                currentHandleHeader = block.getHeader();
                index++;
                logger.info("add block: {}", block.getHeader());
            }
            inspectorHandler.upsertDagInfoFromBlocks(new ArrayList<>(blockMap.values()));

            // Update offset
            localBlockOffset.setBlockHeight(currentHandleHeader.getHeight());
            localBlockOffset.setBlockHash(currentHandleHeader.getBlockHash());
            elasticSearchHandler.setRemoteOffset(localBlockOffset);
            logger.info("indexer update success: {}", localBlockOffset);
        } catch (JSONRPC2SessionException | IOException e) {
            logger.error("chain header error:", e);
        }
    }
}
