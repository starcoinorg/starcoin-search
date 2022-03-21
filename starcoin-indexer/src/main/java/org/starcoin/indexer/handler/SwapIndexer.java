package org.starcoin.indexer.handler;

import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;

import static org.starcoin.utils.DateTimeUtils.getTimeStamp;


public class SwapIndexer extends QuartzJobBean {

    private static final Logger logger = LoggerFactory.getLogger(SwapIndexer.class);
    @Autowired
    private SwapHandle swapHandle;

    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) {
        long endTs = getTimeStamp(0);
        long startTs = getTimeStamp(-1);
        swapHandle.swapStat(startTs, endTs);
        logger.info("swap index handle ok: {} , {}", startTs, endTs);
    }

}
