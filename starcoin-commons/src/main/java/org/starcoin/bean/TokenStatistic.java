package org.starcoin.bean;

import com.alibaba.fastjson.annotation.JSONField;

public class TokenStatistic {
    @JSONField(name = "type_tag")
    private String typeTag;

    private long addressHolder;

    private long volume;

    @JSONField(name = "market_cap")
    private double marketCap;

    public String getTypeTag() {
        return typeTag;
    }

    public void setTypeTag(String typeTag) {
        this.typeTag = typeTag;
    }

    public long getAddressHolder() {
        return addressHolder;
    }

    public void setAddressHolder(long addressHolder) {
        this.addressHolder = addressHolder;
    }

    public long getVolume() {
        return volume;
    }

    public void setVolume(long volume) {
        this.volume = volume;
    }

    public double getMarketCap() {
        return marketCap;
    }

    public void setMarketCap(double marketCap) {
        this.marketCap = marketCap;
    }
}
