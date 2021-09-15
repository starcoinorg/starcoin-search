package org.starcoin.search.bean;

import org.starcoin.bean.OracleTokenPair;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class OracleTokenPrice {

    private Map<String,TreeMap<Long, BigDecimal>> tokenPriceMap =new HashMap<>();

    public OracleTokenPrice(List<OracleTokenPair> oracleTokenPairList){
        for(OracleTokenPair oracleTokenPair:oracleTokenPairList){
            TreeMap<Long,BigDecimal> priceMap = tokenPriceMap.get(oracleTokenPair.getPairName());
            if(priceMap==null){
                priceMap = new TreeMap<>();
                tokenPriceMap.put(oracleTokenPair.getPairName(),priceMap);
            }
            BigDecimal price = new BigDecimal(oracleTokenPair.getLatestPrice());
            priceMap.put(oracleTokenPair.getCreatedAt(),price.movePointLeft(oracleTokenPair.getDecimals()));
        }
    }

    public BigDecimal getPrice(String tokenPairName,long timestamp){
        TreeMap<Long,BigDecimal> priceMap = tokenPriceMap.get(tokenPairName);
        if(priceMap==null){
            return null;
        }
        Map.Entry<Long,BigDecimal> floorEntry = priceMap.floorEntry(timestamp);
        Map.Entry<Long,BigDecimal> higherEntry = priceMap.higherEntry(timestamp);
        if(Math.abs(timestamp-floorEntry.getKey())<=Math.abs(timestamp-higherEntry.getKey())){
            return floorEntry.getValue();
        }else{
            return higherEntry.getValue();
        }
    }
}
