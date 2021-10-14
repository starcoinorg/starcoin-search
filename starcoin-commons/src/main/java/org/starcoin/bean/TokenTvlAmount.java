package org.starcoin.bean;

import com.alibaba.fastjson.annotation.JSONField;

import java.math.BigInteger;

public class TokenTvlAmount {

    @JSONField(name = "token_id")
    private String tokenName;

    @JSONField(name = "total_reserve")
    private BigInteger tvlAmount;

    public TokenTvlAmount(String tokenName, BigInteger tvl) {
        this.tokenName = tokenName;
        this.tvlAmount = tvl;
    }

    public String getTokenName() {
        return tokenName;
    }

    public void setTokenName(String tokenName) {
        this.tokenName = tokenName;
    }

    public BigInteger getTvlAmount() {
        return tvlAmount;
    }

    public void setTvlAmount(BigInteger tvlAmount) {
        this.tvlAmount = tvlAmount;
    }

    @Override
    public String toString() {
        return "TokenTvlAmount{" +
                "tokenName='" + tokenName + '\'' +
                ", tvlAmount=" + tvlAmount +
                '}';
    }
}
