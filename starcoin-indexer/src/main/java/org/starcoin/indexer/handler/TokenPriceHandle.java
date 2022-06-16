package org.starcoin.indexer.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.starcoin.bean.*;
import org.starcoin.constant.StarcoinNetwork;
import org.starcoin.indexer.service.SwapTxnService;
import org.starcoin.indexer.service.TokenPriceService;
import org.starcoin.utils.SwapApiClient;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.starcoin.utils.DateTimeUtils.getTimeStamp;

@Service
public class TokenPriceHandle {
    private static final long AN_HOUR = 60 * 60 * 1000;
    private static final Logger logger = LoggerFactory.getLogger(TokenPriceHandle.class);
    @Value("${starcoin.network}")
    private String network;
    private StarcoinNetwork localNetwork;
    @Autowired
    private SwapApiClient swapApiClient;

    @Autowired
    private TokenPriceService tokenPriceService;
    @Autowired
    private SwapTxnService swapTxnService;

    @PostConstruct
    public void init() {
        //init network
        if (localNetwork == null) {
            localNetwork = StarcoinNetwork.fromValue(network);
        }
    }
    public void statPrice(int date) {
        long begin = getTimeStamp(date - 1);
        long end = getTimeStamp(date);
        List<SwapToken> tokenList = getTokens();
        if(tokenList != null && tokenList.size() > 0) {
            for (SwapToken token: tokenList) {
                String tokenTag = token.getStructTag().toString();
                TokenPriceStat tokenPriceStat = tokenPriceService.getTokenPrice(tokenTag, begin, end);
                logger.info("stat price : {}", tokenPriceStat);
                if(tokenPriceStat != null && tokenPriceStat.getPrice() != null) {
                    tokenPriceService.savePriceStat(tokenPriceStat);
                    logger.info("stat price save ok: {}", tokenPriceStat);
                }else {
                    logger.warn("stat price is null: {}, {}, {}", tokenTag, begin, end);
                }
            }
        }else{
            logger.warn("token list is null");
        }
    }

    public void getPrice(int date) {
        long time = getTimeStamp(date);
        for(int i = 0; i< 24; i++) {
            getHourPrice(time + i * 3600 * 1000);
        }
    }

    public  void getHourPrice(long timestamp) {
        logger.info("get price: {}" ,  timestamp);
        List<SwapToken> tokenList = getTokens();
        Map<String, String> tokenInfos = new HashMap<>();
        Map<String, BigDecimal> tokenPrices = new HashMap<>();

        if(tokenList != null) {
//            get an hour ago price
            List<String> tokens = new ArrayList<>();
            for (SwapToken token: tokenList) {
                String tokenTag = token.getStructTag().toString();
                tokenInfos.put(token.getTokenId(), tokenTag);
                tokens.add(tokenTag);
            }
            try {
               List<OracleTokenPair> pairs = swapApiClient.getProximatePriceRounds(localNetwork.getValue(), tokens, String.valueOf(timestamp));
                if (pairs != null && !pairs.isEmpty()) {
                    List<TokenPrice> tokenPriceList = new ArrayList<>();
                    BigDecimal priceA;
                    for (OracleTokenPair pair: pairs) {
                        if(pair != null) {
                            priceA = new BigDecimal(pair.getPrice());
                            priceA = priceA.movePointLeft(pair.getDecimals());
                            String token = pair.getToken();
                            if(token != null) {
                                String longName = tokenInfos.get(token);
                                if(longName != null) {
                                    tokenPriceList.add(new TokenPrice(longName, timestamp, priceA));
                                    tokenPrices.put(longName, priceA);
                                }else {
                                    logger.warn("to long name err: {}", token);
                                }
                            }else {
                                logger.warn("get token null from pair: {}", pair);
                            }
                        }
                    }
                    //save to db
                    if(tokenPriceList.size() > 0) {
                        tokenPriceService.savePriceList(tokenPriceList);
                        logger.info("save token price ok: {}", tokenPriceList.size());
                        // update swap txn total value
                        long begin = timestamp - AN_HOUR;
                        long end = timestamp;
                        List<SwapTransaction> swapTransactionList =  swapTxnService.getTransactionsByTs(begin, end);
                        if(swapTransactionList != null && swapTransactionList.size() > 0) {
                            for (SwapTransaction swapTxn: swapTransactionList) {
                                BigDecimal total = getTotalValue(swapTxn, tokenPrices);
                                if(total != null) {
                                    swapTxnService.updateTotalValue(total, swapTxn.getSwapSeq());
                                    logger.info("update swap transaction id: {}, total: {}",  swapTxn.getSwapSeq(), total);
                                }else {
                                    logger.warn("get swap transaction total null: {}", swapTxn.getSwapSeq());
                                }
                            }
                        }else {
                            logger.warn("get swap transaction null: {}, {}", begin, end);
                        }
                    }else {
                        logger.warn("token price is null: {}", timestamp);
                    }
                }
            } catch (Exception e) {
                logger.error("get token price error:", e);
            }
        }
    }

    private List<SwapToken> getTokens() {
        List<SwapToken> tokenInfos = new ArrayList<>();
        try {
           tokenInfos = swapApiClient.getTokens(localNetwork.getValue());
        } catch (IOException e) {
            logger.error("get token list error:", e);
        }
        return tokenInfos;
    }

    private BigDecimal getTotalValue(SwapTransaction swapTxn, Map<String, BigDecimal> priceMap) {
        BigDecimal priceA = priceMap.get(swapTxn.getTokenA());
        BigDecimal priceB = priceMap.get(swapTxn.getTokenB());
        boolean isSwap = SwapType.isSwap(swapTxn.getSwapType());
        if (isSwap) {
            if (priceA != null) {
                return priceA.multiply(swapTxn.getAmountA());
            } else if (priceB != null) {
                return priceB.multiply(swapTxn.getAmountB());
            }
        } else {
            BigDecimal total;
            BigDecimal two = new BigDecimal(2);
            if (priceA != null) {
                total = priceA.multiply(swapTxn.getAmountA());
                if (priceB != null) {
                    return total.add(priceB.multiply(swapTxn.getAmountB()));
                } else {
                    return total.multiply(two);
                }
            } else {
                if (priceB != null) {
                    return two.multiply(priceB.multiply(swapTxn.getAmountB()));
                }
            }
        }
        return null;
    }
}
