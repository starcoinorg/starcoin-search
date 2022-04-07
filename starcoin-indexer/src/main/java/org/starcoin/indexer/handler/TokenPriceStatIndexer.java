package org.starcoin.indexer.handler;

import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;

public class TokenPriceStatIndexer extends QuartzJobBean {
    private static final Logger logger = LoggerFactory.getLogger(TokenPriceStatIndexer.class);
    @Autowired
    private TokenPriceHandle tokenPriceHandle;

    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) {
        tokenPriceHandle.statPrice(0);
        logger.info("token price hour handle ok. ");
    }
}