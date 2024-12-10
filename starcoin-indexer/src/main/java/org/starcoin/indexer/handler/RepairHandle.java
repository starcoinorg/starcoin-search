package org.starcoin.indexer.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.starcoin.api.BlockRPCClient;
import org.starcoin.api.Result;
import org.starcoin.api.TransactionRPCClient;
import org.starcoin.bean.Block;
import org.starcoin.bean.BlockHeader;
import com.thetransactioncompany.jsonrpc2.client.JSONRPC2SessionException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RepairHandle {
    private static Logger logger = LoggerFactory.getLogger(RepairHandle.class);

    @Autowired
    private ElasticSearchHandler elasticSearchHandler;

    @Autowired
    private BlockRPCClient blockRPCClient;

    @Autowired
    private TransactionRPCClient transactionRPCClient;

    public void check(long begin, long end) {
        for (long i = begin; i <= end; i++) {
            try {
                //get block from chain
                Block blockOnChain = blockRPCClient.getBlockByHeight(i);
                if (blockOnChain == null) {
                    logger.warn("block not exist on chain: {}", i);
                    continue;
                }
                //get block from es
                Block blockOnEs = elasticSearchHandler.getBlockId(i);
                if (blockOnEs != null) {
                    if (!blockOnChain.getHeader().getBlockHash().equals(blockOnEs.getHeader().getBlockHash())) {
                        System.out.println("check:" + i);
                    }
                } else {
//                    logger.warn("es not exist block:{}", i);
                    System.out.println("not exist: " + i);
                }

            } catch (JSONRPC2SessionException e) {
                logger.error("repair error:", e);
            }
        }
        logger.info("check ok");
    }

    public boolean autoRepair(long startNumber, int count) {
        //read current master height
        try {
            BlockHeader chainHeader = blockRPCClient.getChainHeader();
            long currentMasterNumber = chainHeader.getHeight();
            if (startNumber > currentMasterNumber - 200) {
                logger.info("repair too fast: {}", startNumber);
                return false;
            }
        } catch (JSONRPC2SessionException e) {
            logger.error("get master error: ", e);
            return false;
        }

        List<Block> blocksOnChain;
        try {
            blocksOnChain = blockRPCClient.getBlockListFromHeight(startNumber, count);

        } catch (JSONRPC2SessionException e) {
            logger.error("get blocks on chain error:", e);
            return false;
        }
        if (blocksOnChain.isEmpty()) {
            logger.warn("blocks on chain is empty!");
            return false;
        }
        Result<Block> blockResult = elasticSearchHandler.getBlockIds(startNumber - count, count);
        if (blockResult == null || blockResult.getContents().isEmpty()) {
            logger.warn("blocks on es is empty!");
            return false;
        }
        Map<Long, Block> blockHashMap = new HashMap<>();
        for (Block block : blockResult.getContents()) {
            blockHashMap.put(block.getHeader().getHeight(), block);
        }

        List<Block> blockList = new ArrayList<>();
        long newStartNumber = startNumber;
        for (Block block : blocksOnChain) {
            Block esBlock = blockHashMap.get(block.getHeader().getHeight());
            if (esBlock == null) {
                logger.warn("es block not exist: {}", block.getHeader().getHeight());
                try {
                    ServiceUtils.fetchTransactionsForBlock(transactionRPCClient, block);
                    blockList.add(block);
                } catch (JSONRPC2SessionException e) {
                    logger.error("add block err:", e);
                }
                continue;
            }
            if (!block.getHeader().getBlockHash().equals(esBlock.getHeader().getBlockHash())) {
                // fork block
                try {
                    ServiceUtils.fetchTransactionsForBlock(transactionRPCClient, block);
                    blockList.add(block);
                } catch (JSONRPC2SessionException e) {
                    logger.error("add fix block err:", e);
                }
            } else {
                logger.info("normal block: {}", block.getHeader().getHeight());
            }
            newStartNumber = block.getHeader().getHeight();
        }
        if (!blockList.isEmpty()) {
            elasticSearchHandler.updateBlock(blockList);
            logger.info("repair ok: {}", newStartNumber);
        } else {
            logger.info("not repair:{}", startNumber);
        }
        return true;
    }

    public void repair(long blockNumber) {
        try {
            //get block from chain
            Block blockOnChain = blockRPCClient.getBlockByHeight(blockNumber);
            if (blockOnChain == null) {
                logger.warn("block not exist on chain: {}", blockNumber);
                return;
            }
            //get block from es
            Block blockOnEs = elasticSearchHandler.getBlockId(blockNumber);
            if (blockOnEs != null) {
                if (!blockOnChain.getHeader().getBlockHash().equals(blockOnEs.getHeader().getBlockHash())) {
//                update block
                    List<Block> blockList = new ArrayList<>();
                    ServiceUtils.fetchTransactionsForBlock(transactionRPCClient, blockOnChain);
                    blockList.add(blockOnChain);
                    elasticSearchHandler.updateBlock(blockList);
                    logger.info("repair ok: {}", blockNumber);
                } else {
                    logger.info("not repair");
                }
            } else {
                logger.warn("es not exist block:{}", blockNumber);
            }

        } catch (JSONRPC2SessionException e) {
            logger.error("repair error:", e);
        }
    }
}
