package org.starcoin.search.handler;

<<<<<<< HEAD
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;

import java.util.Calendar;


public class SwapIndexer extends QuartzJobBean {

    @Autowired
    private SwapHandle swapHandle;

    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        long endTs = getTimeStamp(0);
        long startTs = getTimeStamp(-1);

        swapHandle.volumeStats(startTs,endTs);
    }

    static long getTimeStamp(int day) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH) + day, 0, 0, 0);
        return calendar.getTimeInMillis();
    }
=======
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

>>>>>>> db24fe2 (add stats function and price service)

}
