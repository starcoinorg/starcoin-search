package org.starcoin.search.utils;


import org.starcoin.search.repository.TokenVolumeDTO;

import java.math.BigDecimal;
import java.math.BigInteger;

public class NumberUtils {
    public static BigInteger getBigInteger(BigInteger amountA, BigInteger amountB) {
        BigInteger amount = amountA;
        if(amount == null) {
            amount = amountB;
            if(amount == null) {
                amount = BigInteger.ZERO;
            }
        }else {
            if(amountB != null) {
                amount.add(amountB);
            }
        }
        return amount;
    }

    public static BigDecimal getBigDecimal(BigDecimal volumeA, BigDecimal volumeB) {
        BigDecimal volume = volumeA;
        if(volume == null) {
            volume = volumeB;
            if(volume == null) {
                volume = new BigDecimal(0);
            }
        }else {
            if(volumeB != null) {
                volume.add(volumeB);
            }
        }
        return volume;
    }
}
