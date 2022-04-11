package org.starcoin.indexer.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.starcoin.bean.SwapPoolStat;
import org.starcoin.bean.SwapTransaction;
import org.starcoin.bean.TokenStat;
import org.starcoin.indexer.repository.SwapTransactionRepository;
import org.starcoin.indexer.repository.TokenVolumeDTO;
import org.starcoin.utils.NumberUtils;

import java.math.BigDecimal;
import java.util.List;

@Service
public class SwapTxnService {
    private static final Logger logger = LoggerFactory.getLogger(SwapTxnService.class);

    @Autowired
    private SwapTransactionRepository swapTransactionRepository;

    public void save(SwapTransaction swapTransaction) {
        swapTransactionRepository.save(swapTransaction);
    }

    public void updateTotalValue(BigDecimal totalValue, long seq) {
        swapTransactionRepository.updateTotalValue(totalValue, seq);
    }

    public List<SwapTransaction> getAll() {
        return swapTransactionRepository.findAll();
    }

    public List<SwapTransaction> getTransactionsByTs(long begin, long end) {
        return swapTransactionRepository.findSwapTransactionByTs(begin, end);
    }

    public void saveList(List<SwapTransaction> swapTransactionList) {
        swapTransactionRepository.saveAllAndFlush(swapTransactionList);
    }

    public TokenStat getTokenVolume(String token, long startTime, long endTime) {
        TokenStat tokenStat = new TokenStat(token, startTime);
        TokenVolumeDTO tokenA = swapTransactionRepository.getVolumeByTokenA(token, startTime, endTime);
        TokenVolumeDTO tokenB = swapTransactionRepository.getVolumeByTokenB(token, startTime, endTime);
        tokenStat.setVolume(NumberUtils.getBigDecimal(tokenA.getVolume(), tokenB.getVolume()));
        tokenStat.setVolumeAmount(NumberUtils.getBigDecimal(tokenA.getVolumeAmount(), tokenB.getVolumeAmount()));
        logger.info("get token volume: {}, {}, {}", tokenA.getVolume(), tokenB.getVolume(), tokenStat);
        return tokenStat;
    }

    public SwapPoolStat getPoolVolume(String tokenA, String tokenB, long startTime, long endTime) {
        SwapPoolStat poolStat = new SwapPoolStat(tokenA, tokenB, startTime);
        TokenVolumeDTO tokenADTO = swapTransactionRepository.getPoolVolumeA(tokenA, tokenB, startTime, endTime);
        TokenVolumeDTO tokenBDTO = swapTransactionRepository.getPoolVolumeB(tokenA, tokenB, startTime, endTime);
        poolStat.setVolume(NumberUtils.getBigDecimal(tokenADTO.getVolume(), tokenBDTO.getVolume()));
        poolStat.setVolumeAmount(NumberUtils.getBigDecimal(tokenADTO.getVolumeAmount(), tokenBDTO.getVolumeAmount()));
        logger.info("get pool volume: {}, {}", tokenADTO.getVolume(), poolStat);
        return poolStat;
    }
}
