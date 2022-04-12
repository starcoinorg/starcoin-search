package org.starcoin.indexer.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.starcoin.bean.TokenStat;
import org.starcoin.indexer.repository.TokenStatRepository;

import java.util.Date;
import java.util.List;

@Service
public class TokenStatService {
    @Autowired
    private TokenStatRepository tokenStatRepository;

    public void saveAll(List<TokenStat> tokenStatList) {
        tokenStatRepository.saveAll(tokenStatList);
    }

    public TokenStat getTokenStat(long startTime, String tokenName) {
        Date startDate = new Date(startTime);
        return tokenStatRepository.findTokenStatById(startDate, tokenName);
    }

    public List<TokenStat> getTokenStatByDate(long statTime) {
        Date statDate = new Date(statTime);
        return tokenStatRepository.findTokenStatByDate(statDate);
    }

    public List<String> getAllTokens() {
        return tokenStatRepository.getAllToken();
    }

    public void save(TokenStat tokenStat) {
        tokenStatRepository.save(tokenStat);
    }
}
