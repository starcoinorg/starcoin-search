package org.starcoin.search.bean;

import java.math.BigDecimal;

public class TokenTvlAmount {

    private String tokenName;

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
}
