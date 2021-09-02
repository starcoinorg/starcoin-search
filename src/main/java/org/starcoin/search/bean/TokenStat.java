package org.starcoin.search.bean;

import java.math.BigInteger;

public class TokenStat {

    private String token;

    private BigInteger volume;

    private BigInteger tvl;

    public TokenStat(String token, BigInteger volume, BigInteger tvl) {
        this.token = token;
        this.volume = volume;
        this.tvl = tvl;
    }

    public String getToken() {
        return token;
    }

    public BigInteger getVolume() {
        return volume;
    }

    public BigInteger getTvl() {
        return tvl;
    }
}
