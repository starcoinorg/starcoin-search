package org.starcoin.search.bean;

import com.alibaba.fastjson.annotation.JSONField;

public class SwapPoolInfo {
    private String tokenXId;
    private String tokenYId;
    @JSONField(name = "liquidityTokenAddress")
    private String address;

    public String getTokenXId() {
        return tokenXId;
    }

    public void setTokenXId(String tokenXId) {
        this.tokenXId = tokenXId;
    }

    public String getTokenYId() {
        return tokenYId;
    }

    public void setTokenYId(String tokenYId) {
        this.tokenYId = tokenYId;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    @Override
    public String toString() {
        return "SwapPoolInfo{" +
                "tokenXId='" + tokenXId + '\'' +
                ", tokenYId='" + tokenYId + '\'' +
                ", address='" + address + '\'' +
                '}';
    }
}
