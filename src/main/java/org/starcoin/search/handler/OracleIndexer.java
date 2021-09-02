package org.starcoin.search.handler;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Service;

@Service
public class OracleIndexer extends QuartzJobBean {

    private static final Logger logger = LoggerFactory.getLogger(OracleIndexer.class);

    @Autowired
    private OracleTokenPriceService oracleTokenPriceService;

    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        oracleTokenPriceService.fetchAndStoreOracleTokenPrice();
    }

}
