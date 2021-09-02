package org.starcoin.search.handler;

import org.elasticsearch.client.RestHighLevelClient;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.quartz.QuartzJobBean;


public class SwapIndexer extends QuartzJobBean {

    private RestHighLevelClient client;
    private String index;

    @Autowired
    private ElasticSearchHandler elasticSearchHandler;
    @Value("${starcoin.network}")
    private String network;

    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {
    }


}
