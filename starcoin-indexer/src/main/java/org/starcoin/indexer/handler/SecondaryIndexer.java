package org.starcoin.indexer.handler;

import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.starcoin.api.Result;
import org.starcoin.bean.Transfer;
import org.starcoin.bean.TransferOffset;

import java.util.List;


public class SecondaryIndexer extends QuartzJobBean {
    private static final Logger logger = LoggerFactory.getLogger(SecondaryIndexer.class);
    @Value("${starcoin.indexer.bulk_size}")
    private int bulkSize;
    @Autowired
    private TransferHandle transferHandle;

    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) {
        logger.info("secondary index handle...");

//        read transfer offset
        TransferOffset offset = transferHandle.getRemoteOffset();
        if (offset != null) {
            Result<Transfer> result = transferHandle.getRangeTransfers(offset, bulkSize);
            if (result.getTotal() > 0) {
                List<Transfer> transferList = result.getContents();
                transferHandle.bulk(transferList, offset.getOffset()); //and update offset
                logger.info("handle transfer: {}", offset);
            } else {
                logger.warn("get transfer null: {} {}", offset, result.getContents().size());
            }
        } else {
            //init offset
            TransferOffset transferOffset = new TransferOffset();
            transferOffset.setOffset(0);
            transferOffset.setTimestamp("0");
            transferHandle.setRemoteOffset(transferOffset);
            logger.info("first init offset: {}", transferOffset);
        }
    }
}
