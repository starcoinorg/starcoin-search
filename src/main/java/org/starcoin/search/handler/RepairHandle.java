package org.starcoin.search.handler;

import com.thetransactioncompany.jsonrpc2.client.JSONRPC2SessionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.starcoin.api.BlockRPCClient;
import org.starcoin.api.TransactionRPCClient;
import org.starcoin.bean.Block;

import java.util.ArrayList;
import java.util.List;

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
                    ServiceUtils.addBlockToList(transactionRPCClient, blockList, blockOnChain);
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
