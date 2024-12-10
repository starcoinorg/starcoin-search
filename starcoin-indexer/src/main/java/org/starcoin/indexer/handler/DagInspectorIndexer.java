package org.starcoin.indexer.handler;

import org.elasticsearch.client.RestHighLevelClient;
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
import com.thetransactioncompany.jsonrpc2.client.JSONRPC2SessionException;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.LongStream;

import static org.starcoin.constant.Constant.DAG_INSPECTOR_BLOCK_INDEX;

public class DagInspectorIndexer extends QuartzJobBean {

    private static final Logger logger = LoggerFactory.getLogger(DagInspectorIndexer.class);

    @Autowired
    private DagInspectorIndexerHandler inspectorHandler;

    private BlockIndexerOffset blockIndexerOffset;

    @Autowired
    private BlockRPCClient blockRPCClient;

    @Autowired
    private RestHighLevelClient esClient;

    @Value("${starcoin.indexer.bulk_size}")
    private long bulkSize;

    @Value("${starcoin.network}")
    private String network;

    @PostConstruct
    public void initOffset() {
        blockIndexerOffset = new BlockIndexerOffset(
                ServiceUtils.getIndex(network, DAG_INSPECTOR_BLOCK_INDEX),
                blockRPCClient,
                esClient
        );
        blockIndexerOffset.initRemoteOffset();
    }

    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) {
        //read current offset
        if (blockIndexerOffset == null) {
            initOffset();
        }
        BlockOffset remoteBlockOffset = blockIndexerOffset.getRemoteOffset();
        logger.info("current remote offset: {}", remoteBlockOffset);
        if (remoteBlockOffset == null) {
            logger.warn("offset must not null, please check blocks.mapping!!");
            return;
        }

        if (remoteBlockOffset.getBlockHeight() > blockIndexerOffset.getLocalBlockOffsetHeight()) {
            logger.info("indexer equalize chain blocks.");
            return;
        }

        fetchAndProcessBlockSequel();
        // fetchAndProcessBlocksParallel();
    }

    public void fetchAndProcessBlocksParallel() {
        logger.info("Entered");
        try {
            // Read chain header
            BlockHeader chainHeader = blockRPCClient.getChainHeader();

            // Calculate bulk size
            long headHeight = chainHeader.getHeight();
            long bulkNumber = Math.min(headHeight - blockIndexerOffset.getLocalBlockOffsetHeight(), bulkSize);

            ConcurrentHashMap<String, Block> blockMap = new ConcurrentHashMap<>();

            LongStream.rangeClosed(1, bulkNumber).parallel().forEach(index -> {
                long currentBlockHeight = blockIndexerOffset.getLocalBlockOffsetHeight() + index;

                logger.info("Start Get block number: {}, currentBlockHeight: {}", index, currentBlockHeight);
                Block block;
                try {
                    block = blockRPCClient.getBlockByHeight(currentBlockHeight);
                    if (block == null) {
                        logger.warn("get block null: {}", currentBlockHeight);
                        return;
                    }

                    blockMap.put(block.getHeader().getBlockHash(), block);

                    logger.info("add block: {}", block.getHeader());
                } catch (Exception e) {
                    logger.error("Error getting block at height {}: ", currentBlockHeight, e);
                }
            });

            // Process the collected blocks
            inspectorHandler.upsertDagInfoFromBlocks(new ArrayList<>(blockMap.values()));

            // Update offset with the last processed block
            BlockHeader lastBlockHeader = blockMap.values().stream()
                    .map(Block::getHeader)
                    .max(Comparator.comparingLong(BlockHeader::getHeight)).orElseThrow();

            blockIndexerOffset.updateBlockOffset(lastBlockHeader.getHeight(), lastBlockHeader.getBlockHash());

            logger.info("Index update success: {}", blockIndexerOffset);

        } catch (JSONRPC2SessionException | IOException e) {
            logger.error("chain header error:", e);
        } finally {
            logger.info("Exited");
        }

    }

    public void fetchAndProcessBlockSequel() {
        // Read chain header
        try {
            long bulkNumber = Math.min(blockRPCClient.getChainHeader().getHeight() - blockIndexerOffset.getLocalBlockOffsetHeight(), bulkSize);
            int index = 1;
            List<Block> blockList = new ArrayList<>();
            long minHeight =  blockIndexerOffset.getLocalBlockOffsetHeight();
            String currentBlockHash = blockIndexerOffset.getLocalOffsetBlockHash();
            Set<String> visit = new HashSet<>();
            Deque<Block> deque = new ArrayDeque<>();

            long currentBlockHeight = minHeight;

            while (index <= bulkNumber) {
                currentBlockHeight = minHeight + index;

                logger.info("Start Get block number: {}, currentBlockHeight: {}", index, currentBlockHeight);
                Block block = blockRPCClient.getBlockByHeight(currentBlockHeight);
                if (block == null) {
                    logger.warn("get block null: {}", currentBlockHeight);
                    return;
                }
                visit.add(block.getHeader().getBlockHash());
                fetchParentsBlock(block, visit, deque, blockList, minHeight);
                while (!deque.isEmpty()) {
                    int size = deque.size();
                    for (int i = 0; i < size; i++) {
                        Block block_parent = deque.removeFirst();
                        fetchParentsBlock(block_parent, visit, deque, blockList, minHeight);
                    }
                }
                blockList.add(block);
                index++;

                currentBlockHash = block.getHeader().getBlockHash();

                logger.info("add block: {}", block.getHeader());
            }
            inspectorHandler.upsertDagInfoFromBlocks(blockList);

            // Update offset
            blockIndexerOffset.updateBlockOffset(currentBlockHeight, currentBlockHash);

            logger.info("indexer update success: {}", blockIndexerOffset);
        } catch (JSONRPC2SessionException | IOException e) {
            logger.error("chain header error:", e);
        }
    }

    void fetchParentsBlock(
            Block block,
            Set<String> visit,
            Deque<Block> deque,
            List<Block> blockList,
            long minHeight
    ) throws JSONRPC2SessionException {
        List<String> parents = block.getHeader().getParentsHash();
        if (parents == null || parents.isEmpty()) {
            return ;
        }

        for (String parent : block.getHeader().getParentsHash()) {
            if (visit.contains(parent)) {
                continue;
            }
            visit.add(parent);
            Block block_parent = blockList
                    .stream()
                    .filter(b -> b.getHeader().getBlockHash().compareToIgnoreCase(parent) == 0)
                    .findAny()
                    .orElse(null);

            if (block_parent != null) {
                continue;
            }

            block_parent = blockRPCClient.getBlockByHash(parent);
            if (block_parent.getHeader().getHeight() >= minHeight) {
                deque.addLast(block_parent);
                blockList.add(block_parent);
            }
        }
    }
}