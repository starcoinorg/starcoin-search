package org.starcoin.search.bean;

import java.math.BigDecimal;

public class TokenStat {

    private String token;

    private BigDecimal volumeAmount;

    private BigDecimal volume;

    private BigDecimal tvl;

    private BigDecimal tvlAmount;

    public TokenStat(String token, BigDecimal volume,BigDecimal volumeAmount, BigDecimal tvl,BigDecimal tvlAmount) {
        this.token = token;
        this.volume = volume;
        this.volumeAmount = volumeAmount;
        this.tvl = tvl;
        this.tvlAmount = tvlAmount;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public void setVolumeAmount(BigDecimal volumeAmount) {
        this.volumeAmount = volumeAmount;
    }

    public void setVolume(BigDecimal volume) {
        this.volume = volume;
    }

    public void setTvl(BigDecimal tvl) {
        this.tvl = tvl;
    }

    public String getToken() {
        return token;
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

    public BigDecimal getTvlAmount() {
        return tvlAmount;
    }

    public void setTvlAmount(BigDecimal tvlAmount) {
        this.tvlAmount = tvlAmount;
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
