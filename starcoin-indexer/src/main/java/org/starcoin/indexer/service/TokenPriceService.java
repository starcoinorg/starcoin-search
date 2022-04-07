package org.starcoin.indexer.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.starcoin.bean.TokenPrice;
import org.starcoin.bean.TokenPriceStat;
import org.starcoin.indexer.repository.TokenPriceRepository;
import org.starcoin.indexer.repository.TokenPriceStatDTO;
import org.starcoin.indexer.repository.TokenPriceStatRepository;

import java.util.Date;
import java.util.List;

@Service
public class TokenPriceService {
    @Autowired
    private TokenPriceRepository tokenPriceRepository;
    @Autowired
    private TokenPriceStatRepository tokenPriceStatRepository;

    public void savePriceList(List<TokenPrice> tokenPriceList) {
        tokenPriceRepository.saveAllAndFlush(tokenPriceList);
    }

    public void savePriceStat(TokenPriceStat tokenPriceStat) {
        tokenPriceStatRepository.save(tokenPriceStat);
    }

    public TokenPriceStat getTokenPrice(String token, long beginTime, long endTime) {
        TokenPriceStatDTO tokenPriceStatDTO = tokenPriceRepository.getPriceByToken(token, beginTime, endTime);
        if(tokenPriceStatDTO != null) {
            Date date = new Date(endTime);
            return new TokenPriceStat(token, date, tokenPriceStatDTO.getPrice(), tokenPriceStatDTO.getMaxPrice(), tokenPriceStatDTO.getMinPrice());
        }
        return null;
    }
}
