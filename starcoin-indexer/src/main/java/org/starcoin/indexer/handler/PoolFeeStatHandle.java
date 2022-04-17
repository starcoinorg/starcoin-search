package org.starcoin.indexer.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.starcoin.bean.PoolFeeStat;
import org.starcoin.indexer.repository.PoolFeeStatRepository;
import org.starcoin.indexer.service.SwapEventService;

import java.sql.Date;
import java.util.List;

import static org.starcoin.utils.DateTimeUtils.getWholeDatTime;

@Service
public class PoolFeeStatHandle {
    private static final Logger logger = LoggerFactory.getLogger(PoolFeeStatHandle.class);
    @Autowired
    private SwapEventService swapEventService;
    @Autowired
    private PoolFeeStatRepository poolFeeStatRepository;

    public void handle(long statTime) {
        Date statDate = new Date(getWholeDatTime(statTime));
        logger.info("pool fee stat: {}", statDate);
        List<PoolFeeStat> feeList = swapEventService.getFeeStat(statDate, statTime);
        if(feeList != null && feeList.size() > 0) {
            poolFeeStatRepository.saveAll(feeList);
            logger.info("swap pool fee stat handle ok: {} to {} ", statDate, statTime);
        }else {
            logger.warn("swap pool fee stat result is null: {} to {} ", statDate, statTime);
        }
    }
}
