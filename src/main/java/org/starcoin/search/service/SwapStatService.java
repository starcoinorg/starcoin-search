package org.starcoin.search.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.starcoin.search.bean.SwapStat;
import org.starcoin.search.repository.SwapStatRepository;

@Service
public class SwapStatService {
    @Autowired
    private SwapStatRepository swapStatRepository;
    public void save(SwapStat swapStat) {
        swapStatRepository.save(swapStat);
    }
}
