package org.starcoin.indexer.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.starcoin.bean.SwapStat;
import org.starcoin.indexer.repository.SwapStatRepository;

@Service
public class SwapStatService {
    @Autowired
    private SwapStatRepository swapStatRepository;

    public void save(SwapStat swapStat) {
        swapStatRepository.save(swapStat);
    }
}
