package org.starcoin.search.handler;

import com.novi.serde.DeserializationError;
import com.thetransactioncompany.jsonrpc2.client.JSONRPC2SessionException;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.starcoin.api.BlockRPCClient;
import org.starcoin.api.TransactionRPCClient;
import org.starcoin.bean.*;
import org.starcoin.search.bean.Offset;
import org.starcoin.types.TransactionPayload;
import org.starcoin.utils.Hex;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;


public class IndexerHandle extends QuartzJobBean {
    private final static int HANDLE_COUNT = 100;
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
            List<Block> blockList = new ArrayList<>();
            while (index <= bulkNumber) {
                long readNumber = localOffset.getBlockHeight() + index;
                Block block = blockRPCClient.getBlockByHeight(readNumber);
                if (!block.getHeader().getParentHash().equals(currentHandleHeader.getBlockHash())) {
                    //fork occurs
                    logger.warn("Fork detected, roll back: {}, {}, {}", readNumber, block.getHeader().getParentHash(), currentHandleHeader.getBlockHash());
                    //获取上一个index的block
                    Block lastBlock = blockRPCClient.getBlockByHeight(readNumber - 1);
                    if (lastBlock != null && lastBlock.getHeader().getBlockHash().equals(block.getHeader().getParentHash())) {
                        addToList(blockList, lastBlock);
                    } else {
                        logger.warn("fork block not found: {}", lastBlock);
                    }
                    deleteOrSkipIndex = currentHandleHeader.getHeight();
                    logger.warn("fork delete or skip index: {}", deleteOrSkipIndex);
                }
                //set event
                addToList(blockList, block);
                //update current header
                currentHandleHeader = block.getHeader();
                index++;
                logger.debug("add block: {}", block.getHeader());
            }
            //bulk execute
            elasticSearchHandler.bulk(blockList, deleteOrSkipIndex);
            //update offset
            localOffset.setBlockHeight(currentHandleHeader.getHeight());
            localOffset.setBlockHash(currentHandleHeader.getBlockHash());
            elasticSearchHandler.setRemoteOffset(localOffset);
            logger.info("indexer update success: {}", localOffset);
        } catch (JSONRPC2SessionException e) {
            logger.error("chain header error:", e);
        }
    }

    private void addToList(List<Block> blockList, Block block) throws JSONRPC2SessionException {
        List<Transaction> transactionList = transactionRPCClient.getBlockTransactions(block.getHeader().getBlockHash());
        for (Transaction transaction : transactionList) {
            BlockMetadata metadata = null;
            Transaction userTransaction = transactionRPCClient.getTransactionByHash(transaction.getTransactionHash());
            if (userTransaction != null) {
                UserTransaction inner = userTransaction.getUserTransaction();
                metadata = userTransaction.getBlockMetadata();
                if (metadata != null) {
                    transaction.setBlockMetadata(metadata);
                    block.setBlockMetadata(metadata);
                }
                if (inner != null) {
                    try {
                        RawTransaction rawTransaction = inner.getRawTransaction();
                        TransactionPayload payload = TransactionPayload.bcsDeserialize(Hex.decode(rawTransaction.getPayload()));
                        if (TransactionPayload.Script.class.equals(payload.getClass())) {
                            transaction.setTransactionType(TransactionType.Script);
                        } else if (TransactionPayload.Package.class.equals(payload.getClass())) {
                            transaction.setTransactionType(TransactionType.Package);
                        } else if (TransactionPayload.ScriptFunction.class.equals(payload.getClass())) {
                            transaction.setTransactionType(TransactionType.ScriptFunction);
                        } else {
                            logger.warn("payload class error: {}", payload.getClass());
                        }
                        rawTransaction.setTransactionPayload(payload);
                        inner.setRawTransaction(rawTransaction);
                        transaction.setUserTransaction(inner);
                    } catch (DeserializationError deserializationError) {
                        logger.error("Deserialization payload error:", deserializationError);
                    }
                }
            } else {
                logger.warn("get txn by hash is null: {}", transaction.getTransactionHash());
            }
            transaction.setTimestamp(block.getHeader().getTimestamp());
            // set events
            List<Event> events = transactionRPCClient.getTransactionEvents(transaction.getTransactionHash());
            if (!events.isEmpty()) {
                transaction.setEvents(events);
            } else {
                logger.warn("current txn event is null: {}", transaction.getTransactionHash());
            }
        }
        block.setTransactionList(transactionList);
        blockList.add(block);
    }

}