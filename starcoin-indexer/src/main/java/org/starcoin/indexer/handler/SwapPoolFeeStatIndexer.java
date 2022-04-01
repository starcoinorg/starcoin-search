package org.starcoin.indexer.handler;

import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.starcoin.bean.PoolFeeStat;
import org.starcoin.indexer.repository.PoolFeeStatRepository;
import org.starcoin.indexer.service.SwapEventService;

import java.sql.Date;
import java.util.List;

import static org.starcoin.utils.DateTimeUtils.getTimeStamp;

public class SwapPoolFeeStatIndexer extends QuartzJobBean {
    private static final Logger logger = LoggerFactory.getLogger(SwapPoolFeeStatIndexer.class);
    @Autowired
    private SwapEventService swapEventService;
    @Autowired
    private PoolFeeStatRepository poolFeeStatRepository;

    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) {
        Date fromDate = new Date(getTimeStamp(-2));
        Date toDate = new Date(getTimeStamp(0));
        List<PoolFeeStat> feeList = swapEventService.getFeeStat(fromDate, toDate);
        if(feeList != null && feeList.size() > 0) {
            poolFeeStatRepository.saveAll(feeList);
            logger.info("swap pool fee stat handle ok: {} to {} ", fromDate, toDate);
        }else {
            logger.warn("swap pool fee stat result is null: {} to {} ", fromDate, toDate);
        }
    }
}