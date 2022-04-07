package org.starcoin.indexer.handler;

import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;

import static org.starcoin.utils.DateTimeUtils.getAnHourAgo;

public class TokenPriceHourIndexer extends QuartzJobBean {
    private static final Logger logger = LoggerFactory.getLogger(TokenPriceHourIndexer.class);
    @Autowired
    private TokenPriceHandle tokenPriceHandle;

    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) {
        tokenPriceHandle.getHourPrice(getAnHourAgo());
        logger.info("token price hour handle ok. ");
    }
}
