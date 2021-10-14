package org.starcoin.bean;

import com.alibaba.fastjson.annotation.JSONField;

import java.math.BigInteger;

public class TokenHolderInfo {

    private String address;
    private BigInteger supply;

    @JSONField(name = "amount")
    private BigInteger holdAmount;

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public BigInteger getSupply() {
        return supply;
    }

    public void setSupply(BigInteger supply) {
        this.supply = supply;
    }

    public BigInteger getHoldAmount() {
        return holdAmount;
    }

    public void setHoldAmount(BigInteger holdAmount) {
        this.holdAmount = holdAmount;
    }
}
