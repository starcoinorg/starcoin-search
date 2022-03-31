package org.starcoin.indexer.handler;

import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;

public class SwapEventIndexer extends QuartzJobBean {

    @Autowired
    private SwapEventHandle swapEventHandle;
    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) {
        swapEventHandle.handle();
    }
}
