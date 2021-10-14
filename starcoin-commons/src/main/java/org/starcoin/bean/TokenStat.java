package org.starcoin.bean;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;

@Entity
@Table(name = "token_swap_day_stat")
public class TokenStat {

    @EmbeddedId
    TokenStatId tokenStatId;
    @Column(name = "volume_amount")
    private BigDecimal volumeAmount;
    @Column(name = "volume")
    private BigDecimal volume;
    @Column(name = "tvl")
    private BigDecimal tvl;
    @Column(name = "tvl_amount")
    private BigInteger tvlAmount;

    public TokenStat() {
    }

    public TokenStat(String token, long timestamp) {
        TokenStatId tokenStatId = new TokenStatId();
        tokenStatId.setToken(token);
        tokenStatId.setTimestamp(new Date(timestamp));
        this.tokenStatId = tokenStatId;
    }

    public TokenStat(BigDecimal volume, BigDecimal volumeAmount, BigDecimal tvl, BigInteger tvlAmount) {
        this.volume = volume;
        this.volumeAmount = volumeAmount;
        this.tvl = tvl;
        this.tvlAmount = tvlAmount;
    }

    public BigDecimal getVolume() {
        return volume;
    }

    public void setVolume(BigDecimal volume) {
        this.volume = volume;
    }

    public BigDecimal getTvl() {
        return tvl;
    }

    public void setTvl(BigDecimal tvl) {
        this.tvl = tvl;
    }

    public BigDecimal getVolumeAmount() {
        return volumeAmount;
    }

    public void setVolumeAmount(BigDecimal volumeAmount) {
        this.volumeAmount = volumeAmount;
    }

    public BigInteger getTvlAmount() {
        return tvlAmount;
    }

    public void setTvlAmount(BigInteger tvlAmount) {
        this.tvlAmount = tvlAmount;
    }

    public void addVolumeAmount(BigDecimal bigDecimal) {
        volumeAmount.add(bigDecimal);
    }

    public void addVolume(BigDecimal bigDecimal) {
        volume.add(bigDecimal);
    }

    public void add(TokenStat tokenStat) {
        volume.add(tokenStat.getVolume());
        volumeAmount.add(tokenStat.getVolumeAmount());
    }

    public TokenStatId getTokenStatId() {
        return tokenStatId;
    }

    public void setTokenStatId(TokenStatId tokenStatId) {
        this.tokenStatId = tokenStatId;
    }

    public String getToken() {
        return tokenStatId.getToken();
    }

    @Override
    public String toString() {
        return "TokenStat{" +
                ", volumeAmount=" + volumeAmount +
                ", volume=" + volume +
                ", tvl=" + tvl +
                ", tvlAmount=" + tvlAmount +
                '}';
    }
}

@Embeddable
class TokenStatId implements Serializable {
    @Column(name = "token_name")
    private String token;
    @Column(name = "ts")
    private Date timestamp;

    public TokenStatId(String token, Date timestamp) {
        this.token = token;
        this.timestamp = timestamp;
    }

    public TokenStatId() {
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
}
