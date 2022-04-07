package org.starcoin.indexer.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.starcoin.bean.OracleTokenPair;
import org.starcoin.bean.SwapToken;
import org.starcoin.bean.TokenPrice;
import org.starcoin.bean.TokenPriceStat;
import org.starcoin.constant.StarcoinNetwork;
import org.starcoin.indexer.service.TokenPriceService;
import org.starcoin.utils.SwapApiClient;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.starcoin.constant.Constant.STC_TOKEN_OR_TAG;
import static org.starcoin.utils.DateTimeUtils.getTimeStamp;

@Service
public class TokenPriceHandle {
    private static final Logger logger = LoggerFactory.getLogger(TokenPriceHandle.class);
    @Value("${starcoin.network}")
    private String network;
    private StarcoinNetwork localNetwork;
    @Autowired
    private SwapApiClient swapApiClient;

    @Autowired
    private TokenPriceService tokenPriceService;

    public void statPrice(int date) {
        long begin = getTimeStamp(date - 1);
        long end = getTimeStamp(date);
       TokenPriceStat tokenPriceStat = tokenPriceService.getTokenPrice(STC_TOKEN_OR_TAG, begin, end);
       logger.info("stat price : {}", tokenPriceStat);
       if(tokenPriceStat != null) {
           tokenPriceService.savePriceStat(tokenPriceStat);
           logger.info("stat price save ok: {}", tokenPriceStat);
       }else {
           logger.warn("stat price is null: {}, {}", begin, end);
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
        //init network
        if (localNetwork == null) {
            localNetwork = StarcoinNetwork.fromValue(network);
        }
        List<SwapToken> tokenList = null;
        Map<String, String> tokenInfos = new HashMap<>();

        try {
             tokenList = swapApiClient.getTokens(localNetwork.getValue());
            for (SwapToken token: tokenList) {
                tokenInfos.put(token.getTokenId(), token.getStructTag().toString());
            }
        } catch (IOException e) {
            logger.error("get token list error:", e);
        }
        if(tokenList != null) {
//            get an hour ago price
            List<String> tokens = new ArrayList<>();
            for (SwapToken token: tokenList) {
                tokens.add(tokenInfos.get(token.getTokenId()));
            }
        //temp add stc
//        tokenInfos.put("STC", STC_TOKEN_OR_TAG);
//        tokens.add(STC_TOKEN_OR_TAG);
            try {
//                long timestamp = getAnHourAgo();
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
                    }
                }
            } catch (IOException e) {
                logger.error("get token price error:", e);
            }
        }

    }
}
