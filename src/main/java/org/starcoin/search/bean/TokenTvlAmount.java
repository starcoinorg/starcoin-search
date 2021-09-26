package org.starcoin.search.bean;

import com.alibaba.fastjson.annotation.JSONField;

import java.math.BigDecimal;

public class TokenTvlAmount {

    @JSONField(name = "token_id")
    private String tokenName;

    @JSONField(name = "total_reserve")
    private BigDecimal tvlAmount;

    public TokenTvlAmount(String tokenName, BigDecimal tvl) {
        this.tokenName = tokenName;
        this.tvlAmount = tvl;
    }

    public String getTokenName() {
        return tokenName;
    }

    public void setTokenName(String tokenName) {
        this.tokenName = tokenName;
    }

    public BigDecimal getTvlAmount() {
        return tvlAmount;
    }

    public void setTvlAmount(BigDecimal tvlAmount) {
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
