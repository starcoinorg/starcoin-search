package org.starcoin.indexer.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.starcoin.bean.SwapPoolStat;
import org.starcoin.indexer.repository.SwapPoolStatRepository;

import java.util.Date;
import java.util.List;

@Service
public class SwapPoolStatService {
    @Autowired
    private SwapPoolStatRepository swapPoolStatRepository;

    public void saveAll(List<SwapPoolStat> poolStatList) {
        swapPoolStatRepository.saveAll(poolStatList);
    }

    public void save(SwapPoolStat poolStat) {
        swapPoolStatRepository.save(poolStat);
    }

    public SwapPoolStat getSwapPoolStat(String tokenA, String tokenB, Date statDate) {
        return swapPoolStatRepository.findSwapPoolStatById(tokenA, tokenB, statDate);
    }

    public List<SwapPoolStat> getSwapPoolStatByDate(Date statDate) {
        return swapPoolStatRepository.findSwapPoolStatByDate(statDate);
    }
}
