package org.starcoin.search.handler;

import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.starcoin.api.Result;
import org.starcoin.bean.PendingTransaction;

import java.util.ArrayList;
import java.util.List;


public class PendingTxnCleanHandle extends QuartzJobBean {
    private final static int HANDLE_COUNT = 100;
    private static Logger LOG = LoggerFactory.getLogger(PendingTxnCleanHandle.class);

    @Autowired
    private ElasticSearchHandler elasticSearchHandler;

    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) {
        Result<PendingTransaction> result = elasticSearchHandler.getPendingTransaction(HANDLE_COUNT);
        List<PendingTransaction> txns = result.getContents();
        if (txns.size() > 0) {
            List<PendingTransaction> deletings = new ArrayList<>();
            for (PendingTransaction pending : txns) {
                String expire_str = pending.getRawTransaction().getExpirationTimestampSecs();
                try {
                    long expire = Long.parseLong(expire_str);
                    long currentTime = System.currentTimeMillis() / 1000;
                    if (expire < currentTime) {
                        deletings.add(pending);
                        LOG.info("deleting pending transaction: {}", pending.getTransactionHash());
                    } else {
                        LOG.info("pending transaction is not expire: {}, {}", expire, currentTime);
                    }
                } catch (NumberFormatException e) {
                    LOG.error("get expire time error:", e);
                }
            }
            if (deletings.size() > 0) {
                elasticSearchHandler.deletePendingTransaction(deletings);
                LOG.info("delete expire pending transaction ok: {}", deletings.size());
            }

        } else {
            LOG.info("current pending transaction is null");
        }
    }
}