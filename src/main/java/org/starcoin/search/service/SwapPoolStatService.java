package org.starcoin.search.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.starcoin.search.bean.SwapPoolStat;
import org.starcoin.search.repository.SwapPoolStatRepository;

import java.util.List;

@Service
public class SwapPoolStatService {
    @Autowired
    private SwapPoolStatRepository swapPoolStatRepository;

    public void saveAll(List<SwapPoolStat> poolStatList) {
        swapPoolStatRepository.saveAll(poolStatList);
    }
}
