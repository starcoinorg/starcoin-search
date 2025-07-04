package org.starcoin.indexer.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.starcoin.api.BlockRPCClient;
import org.starcoin.api.TransactionRPCClient;
import org.starcoin.bean.Block;
import org.starcoin.bean.BlockHeader;
import org.starcoin.bean.BlockOffset;
import org.starcoin.jsonrpc.client.JSONRPC2SessionException;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;


public class IndexerHandle extends QuartzJobBean {
    private static final Logger logger = LoggerFactory.getLogger(IndexerHandle.class);

    private BlockOffset localBlockOffset;
    private BlockHeader currentHandleHeader;

    @Value("${starcoin.network}")
    private String network;

    @Value("${starcoin.indexer.bulk_size}")
    private long bulkSize;

    @Autowired
    private ElasticSearchHandler elasticSearchHandler;

    @Autowired
    private TransactionRPCClient transactionRPCClient;

    @Autowired
    private BlockRPCClient blockRPCClient;

    @PostConstruct
    public void initOffset() throws JSONRPC2SessionException {
        localBlockOffset = elasticSearchHandler.getRemoteOffset();

        // Case 1: Obtained
        if (localBlockOffset != null) {
            Block block = blockRPCClient.getBlockByHeight(localBlockOffset.getBlockHeight());
            if (block != null) {
                currentHandleHeader = block.getHeader();
            } else {
                logger.error("init offset block not exist on chain: {}", localBlockOffset);
            }
            return;
        }

        // Case 2: Unable to obtain
        logger.warn("offset is null,init reset to genesis");
        currentHandleHeader = blockRPCClient.getBlockByHeight(0).getHeader();
        localBlockOffset = new BlockOffset(0, currentHandleHeader.getBlockHash());
        elasticSearchHandler.setRemoteOffset(localBlockOffset);

        logger.info("init offset ok: {}", localBlockOffset);

        // Case 3: Throw an exception
    }

    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) {
        //read current offset
        if (localBlockOffset == null || currentHandleHeader == null) {
            logger.warn("local offset error, reset it: {}, {}", localBlockOffset, currentHandleHeader);
            try {
                initOffset();
            } catch (JSONRPC2SessionException e) {
                logger.error("init offset error, {}", localBlockOffset, e);
            }

            if (localBlockOffset == null && network.compareToIgnoreCase("main") == 0) {
                // Here as long as it is null, it means that the initialization failed, return and wait for the next processing
                logger.error("Init offset error, skip and w");
                return;
            }
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
            //calculate bulk size
            long headHeight = chainHeader.getHeight();
            long bulkNumber = Math.min(headHeight - localBlockOffset.getBlockHeight(), bulkSize);
            int index = 1;
            List<Block> blockList = new ArrayList<>();
            while (index <= bulkNumber) {
                long readNumber = localBlockOffset.getBlockHeight() + index;
                Block block = blockRPCClient.getBlockByHeight(readNumber);
                if (!block.getHeader().getParentHash().equals(currentHandleHeader.getBlockHash())) {
                    //fork handle until reach forked point block
                    logger.warn("Fork detected, roll back: {}, {}, {}", readNumber, block.getHeader().getParentHash(), currentHandleHeader.getBlockHash());
                    Block lastForkBlock, lastMasterBlock;
                    BlockHeader forkHeader = currentHandleHeader;
                    long lastMasterNumber = readNumber - 1;
                    String forkHeaderParentHash = null;
                    do {
                        //获取分叉的block
                        if (forkHeaderParentHash == null) {
                            //第一次先回滚当前最高的分叉块
                            forkHeaderParentHash = forkHeader.getBlockHash();
                        } else {
                            forkHeaderParentHash = forkHeader.getParentHash();
                        }
                        lastForkBlock = elasticSearchHandler.getBlockContent(forkHeaderParentHash);
                        if (lastForkBlock == null) {
                            logger.warn("get fork block null: {}", forkHeaderParentHash);
                            //read from node
                            lastForkBlock = blockRPCClient.getBlockByHash(forkHeaderParentHash);
                        }
                        if (lastForkBlock != null) {
                            elasticSearchHandler.bulkForkedUpdate(lastForkBlock);
                            logger.info("rollback forked block ok: {}, {}", lastForkBlock.getHeader().getHeight(), forkHeaderParentHash);
                        } else {
                            //如果块取不到，先退出当前任务，下一个轮询周期再执行
                            logger.warn("get forked block is null: {}", forkHeaderParentHash);
                            return;
                        }

                        //获取上一个高度主块
                        lastMasterBlock = blockRPCClient.getBlockByHeight(lastMasterNumber);
                        if (lastMasterBlock != null) {
                            long forkNumber = forkHeader.getHeight();
                            logger.info("fork number: {}", forkNumber);
                            forkHeader = lastForkBlock.getHeader();
                            //reset offset to handled fork block
                            currentHandleHeader = forkHeader;
                            localBlockOffset.setBlockHeight(currentHandleHeader.getHeight());
                            localBlockOffset.setBlockHash(currentHandleHeader.getBlockHash());
                            elasticSearchHandler.setRemoteOffset(localBlockOffset);
                            if (lastMasterNumber == forkNumber && lastMasterBlock.getHeader().getBlockHash().equals(forkHeaderParentHash)) {
                                //find fork point
                                logger.info("find fork height: {}", lastMasterNumber);
                                break;
                            }
                            //继续找下一个分叉
                            lastMasterNumber--;
                            logger.info("continue last forked block: {}", lastMasterNumber);
                        } else {
                            logger.warn("get last master Block null: {}", lastMasterNumber);
                        }
                    } while (true);

                    logger.info("rollback handle ok: {}", localBlockOffset);
                    return; //退出当前任务，重新添加从分叉点之后的block
                }
                //set event
                ServiceUtils.addBlockToList(transactionRPCClient, blockList, block);
                //update current header
                currentHandleHeader = block.getHeader();
                index++;
                logger.debug("add block: {}", block.getHeader());
            }
            //bulk execute
            elasticSearchHandler.bulk(blockList);
            //update offset
            localBlockOffset.setBlockHeight(currentHandleHeader.getHeight());
            localBlockOffset.setBlockHash(currentHandleHeader.getBlockHash());
            elasticSearchHandler.setRemoteOffset(localBlockOffset);
            logger.info("indexer update success: {}", localBlockOffset);
        } catch (JSONRPC2SessionException | JsonProcessingException e) {
            logger.error("chain header error:", e);
        }
    }
}