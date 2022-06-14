package org.starcoin.bean;

import com.alibaba.fastjson.annotation.JSONField;

import java.math.BigInteger;

public class TokenMarketCap {

    @JSONField(name = "type_tag")
    private String typeTag;

    private BigInteger marketCap;

    public TokenMarketCap(String token, BigInteger marketCap) {
        this.typeTag = token;
        this.marketCap = marketCap;
    }

    public String getTypeTag() {
        return typeTag;
    }

    public void setTypeTag(String typeTag) {
        this.typeTag = typeTag;
    }

    public BigInteger getMarketCap() {
        return marketCap;
    }

    public void setMarketCap(BigInteger marketCap) {
        this.marketCap = marketCap;
    }

    @Override
    public String toString() {
        return "TokenMarketCap{" +
                "typeTag='" + typeTag + '\'' +
                ", marketCap=" + marketCap +
                '}';
    }
}
