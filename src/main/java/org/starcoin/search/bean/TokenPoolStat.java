package org.starcoin.search.bean;

import com.alibaba.fastjson.annotation.JSONField;

import java.math.BigDecimal;

public class TokenPoolStat {

    @JSONField(name = "token_pair")
    private TokenPair tokenPair;

    private BigDecimal volume;

    private BigDecimal volumeAmount;

    private BigDecimal tvl;

    public TokenPair getTokenPair() {
        return tokenPair;
    }

    public BigDecimal getVolume() {
        return volume;
    }

    public BigDecimal getTvl() {
        return tvl;
    }

    public BigDecimal getVolumeAmount() {
        return volumeAmount;
    }

    public void setTokenPair(TokenPair tokenPair) {
        this.tokenPair = tokenPair;
    }

    public void setVolume(BigDecimal volume) {
        this.volume = volume;
    }

    public void setVolumeAmount(BigDecimal volumeAmount) {
        this.volumeAmount = volumeAmount;
    }

    public void setTvl(BigDecimal tvl) {
        this.tvl = tvl;
    }

    public TokenPoolStat(TokenPair tokenPair, BigDecimal volume, BigDecimal volumeAmount, BigDecimal tvl) {
        this.tokenPair = tokenPair;
        this.volume = volume;
        this.volumeAmount = volumeAmount;
        this.tvl = tvl;
    }

    public void addVolumeAmount(BigDecimal bigDecimal){
        volumeAmount.add(bigDecimal);
    }

    public void addVolume(BigDecimal bigDecimal){
        volume.add(bigDecimal);
    }

    public void add(TokenStat tokenStat){
        volume.add(tokenStat.getVolume());
        volumeAmount.add(tokenStat.getVolumeAmount());
    }

}
