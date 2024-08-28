package org.starcoin.indexer.handler;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.starcoin.bean.TokenMarketCap;
import org.starcoin.indexer.service.AddressHolderService;

import java.util.List;

public class MarketCapIndexer extends QuartzJobBean {
    private static final Logger logger = LoggerFactory.getLogger(MarketCapIndexer.class);

    @Autowired
    private MarketCapHandle handle;

    @Autowired
    private AddressHolderService addressHolderService;

    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        //get token
        try {
            List<TokenMarketCap> tokenMarketCapList = addressHolderService.getMarketCap();
            handle.bulk(tokenMarketCapList);
            logger.info("handle market OK");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
