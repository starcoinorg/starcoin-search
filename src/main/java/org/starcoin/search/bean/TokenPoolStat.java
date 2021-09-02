package org.starcoin.search.bean;

import com.alibaba.fastjson.annotation.JSONField;

import java.math.BigInteger;

public class TokenPoolStat {

    @JSONField(name = "token_pair")
    private TokenPair tokenPair;

    private BigInteger volume;

    private BigInteger tvl;

    public TokenPair getTokenPair() {
        return tokenPair;
    }

    public BigInteger getVolume() {
        return volume;
    }

    public BigInteger getTvl() {
        return tvl;
    }
}
