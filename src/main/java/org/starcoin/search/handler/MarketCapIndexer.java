package org.starcoin.search.handler;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.starcoin.api.Result;
import org.starcoin.search.bean.TokenMarketCap;

public class MarketCapIndexer extends QuartzJobBean {
    private static final Logger logger = LoggerFactory.getLogger(MarketCapIndexer.class);
    @Autowired
    private MarketCapHandle handle;

    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        //get token
        Result<TokenMarketCap> tokenMarketCapResult = handle.getTokenMarketCap();
        handle.bulk(tokenMarketCapResult);
        logger.info("handle market OK");
    }
}
