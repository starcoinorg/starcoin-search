package org.starcoin.search.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.starcoin.search.bean.TokenStat;
import org.starcoin.search.repository.TokenStatRepository;

import java.util.List;

@Service
public class TokenStatService {
    @Autowired
    private TokenStatRepository tokenStatRepository;

    public void saveAll(List<TokenStat> tokenStatList) {
        tokenStatRepository.saveAll(tokenStatList);
    }
}
