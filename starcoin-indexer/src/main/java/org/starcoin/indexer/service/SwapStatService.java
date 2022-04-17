package org.starcoin.indexer.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.starcoin.bean.SwapStat;
import org.starcoin.indexer.repository.SwapStatRepository;

import java.math.BigDecimal;
import java.util.Date;

@Service
public class SwapStatService {
    @Autowired
    private SwapStatRepository swapStatRepository;

    public void save(SwapStat swapStat) {
        swapStatRepository.save(swapStat);
    }

    public void updateTvl(Date date, BigDecimal tvl) {
        swapStatRepository.updateTvl(date, tvl);
    }

    public SwapStat get(long date) {
        Date statDate = new Date(date);
        return swapStatRepository.findTokenStatByDate(statDate);
    }
}
