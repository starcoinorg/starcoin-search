package org.starcoin.search.handler;

import com.thetransactioncompany.jsonrpc2.client.JSONRPC2SessionException;
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
import org.starcoin.search.bean.Offset;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class IndexerHandle extends QuartzJobBean {
    private static Logger logger = LoggerFactory.getLogger(IndexerHandle.class);

    private Offset localOffset;
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
    public void initOffset() {
        localOffset = elasticSearchHandler.getRemoteOffset();
        //update current handle header
        try {
            if (localOffset != null) {
                currentHandleHeader = blockRPCClient.getBlockByHeight(localOffset.getBlockHeight()).getHeader();
            } else {
                logger.warn("offset is null,init reset to genesis");
                currentHandleHeader = blockRPCClient.getBlockByHeight(0).getHeader();
                localOffset = new Offset(0, currentHandleHeader.getBlockHash());
                elasticSearchHandler.setRemoteOffset(localOffset);
                logger.info("init offset ok: {}", localOffset);
            }
        } catch (JSONRPC2SessionException e) {
            logger.error("set current header error:", e);
        }
    }

    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) {
        //read current offset
        if (localOffset == null || currentHandleHeader == null) {
//            logger.warn("local offset error, reset it: {}, {}", localOffset, currentHandleHeader);
            initOffset();
        }
        Offset remoteOffset = elasticSearchHandler.getRemoteOffset();
        logger.info("current remote offset: {}", remoteOffset);
        if (remoteOffset == null) {
            logger.warn("offset must not null, please check blocks.mapping!!");
            return;
        }
        if (remoteOffset.getBlockHeight() > localOffset.getBlockHeight()) {
            logger.info("indexer equalize chain blocks.");
            return;
        }
        //read head
        try {
            BlockHeader chainHeader = blockRPCClient.getChainHeader();
            //calculate bulk size
            long headHeight = chainHeader.getHeight();
            long bulkNumber = Math.min(headHeight - localOffset.getBlockHeight(), bulkSize);
            int index = 1;
            long deleteOrSkipIndex = 0;
            Set<Long> deleteForkBlockIds = new HashSet<>();
            List<Block> blockList = new ArrayList<>();
            while (index <= bulkNumber) {
                long readNumber = localOffset.getBlockHeight() + index;
                Block block = blockRPCClient.getBlockByHeight(readNumber);
                if (!block.getHeader().getParentHash().equals(currentHandleHeader.getBlockHash())) {
                    //fork occurs
                    logger.warn("Fork detected, roll back: {}, {}, {}", readNumber, block.getHeader().getParentHash(), currentHandleHeader.getBlockHash());
                    Block lastForkBlock, lastMasterBlock;
                    BlockHeader forkHeader = currentHandleHeader;
                    long lastMasterNumber = readNumber - 1;
                    String forkHeaderParentHash;
                    deleteForkBlockIds.add(currentHandleHeader.getHeight());
                    do {
                        //获取上一个block
                        lastMasterBlock = blockRPCClient.getBlockByHeight(lastMasterNumber);
                        if( lastMasterBlock != null) {
                            // add master block to es
                            ServiceUtils.addBlockToList(transactionRPCClient, blockList, lastMasterBlock);
                            logger.info("add master block:{}", lastMasterNumber);
                            forkHeaderParentHash = forkHeader.getParentHash();
                            long forkNumber = forkHeader.getHeight();
                            logger.info("fork number: {}", forkNumber);
                            if(lastMasterNumber == forkNumber && lastMasterBlock.getHeader().getBlockHash().equals(forkHeaderParentHash)) {
                                //find fork point
                                logger.info("find fork height: {}", lastMasterNumber);
                                break;
                            }else {
                                lastForkBlock = elasticSearchHandler.getBlockContent(forkHeaderParentHash);
                                if(lastForkBlock != null) {
                                    forkHeader = lastForkBlock.getHeader();
                                    deleteForkBlockIds.add(lastMasterNumber);
                                    lastMasterNumber --;
                                }else {
                                    logger.warn("get last fork block null: {}", forkHeaderParentHash);
                                }
                            }
                        }else {
                            logger.warn("get last aster Block null: {}", lastMasterNumber);
                        }
                    }while (true);
                }
                //set event
                ServiceUtils.addBlockToList(transactionRPCClient, blockList, block);
                //update current header
                currentHandleHeader = block.getHeader();
                index++;
                logger.debug("add block: {}", block.getHeader());
            }
            //bulk execute
            elasticSearchHandler.bulk(blockList, deleteForkBlockIds);
            //update offset
            localOffset.setBlockHeight(currentHandleHeader.getHeight());
            localOffset.setBlockHash(currentHandleHeader.getBlockHash());
            elasticSearchHandler.setRemoteOffset(localOffset);
            logger.info("indexer update success: {}", localOffset);
        } catch (JSONRPC2SessionException e) {
            logger.error("chain header error:", e);
        }
    }
}