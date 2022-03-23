package org.starcoin.indexer.handler;

import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;

public class TransactionInfoIndexer  extends QuartzJobBean {
    @Autowired
    private TransactionInfoHandle handle;

    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) {
        handle.handle();
    }
}
