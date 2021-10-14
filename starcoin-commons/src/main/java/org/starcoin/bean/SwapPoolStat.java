package org.starcoin.bean;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;

@Entity
@Table(name = "pool_swap_day_stat")
public class SwapPoolStat {

    @EmbeddedId
    PoolStatId id;
    @Column(name = "volume_amount")
    private BigDecimal volumeAmount;
    @Column(name = "volume")
    private BigDecimal volume;
    @Column(name = "tvl_a")
    private BigDecimal tvlA;
    @Column(name = "tvl_a_amount")
    private BigInteger tvlAAmount;
    @Column(name = "tvl_b")
    private BigDecimal tvlB;
    @Column(name = "tvl_b_amount")
    private BigInteger tvlBAmount;

    public SwapPoolStat(String tokenA, String tokenB, long timestamp) {
        PoolStatId id = new PoolStatId(tokenA, tokenB, timestamp);
        this.id = id;
    }

    public SwapPoolStat() {
    }

    public BigDecimal getVolumeAmount() {
        return volumeAmount;
    }

    public void setVolumeAmount(BigDecimal volumeAmount) {
        this.volumeAmount = volumeAmount;
    }

    public BigDecimal getVolume() {
        return volume;
    }

    public void setVolume(BigDecimal volume) {
        this.volume = volume;
    }

    public BigDecimal getTvlA() {
        return tvlA;
    }

    public void setTvlA(BigDecimal tvlA) {
        this.tvlA = tvlA;
    }

    public BigInteger getTvlAAmount() {
        return tvlAAmount;
    }

    public void setTvlAAmount(BigInteger tvlAAmount) {
        this.tvlAAmount = tvlAAmount;
    }

    public BigDecimal getTvlB() {
        return tvlB;
    }

    public void setTvlB(BigDecimal tvlB) {
        this.tvlB = tvlB;
    }

    public BigInteger getTvlBAmount() {
        return tvlBAmount;
    }

    public void setTvlBAmount(BigInteger tvlBAmount) {
        this.tvlBAmount = tvlBAmount;
    }

    public PoolStatId getId() {
        return id;
    }

    public void setId(PoolStatId id) {
        this.id = id;
    }

}

@Embeddable
class PoolStatId implements Serializable {
    @Column(name = "first_token_name")
    private String tokenA;
    @Column(name = "second_token_name")
    private String tokenB;
    @Column(name = "ts")
    private Date timestamp;

    public PoolStatId(String tokenA, String tokenB, long timestamp) {
        this.tokenA = tokenA;
        this.tokenB = tokenB;
        this.timestamp = new Date(timestamp);
    }

    public PoolStatId() {
    }

    public String getTokenA() {
        return tokenA;
    }

    public void setTokenA(String tokenA) {
        this.tokenA = tokenA;
    }

    public String getTokenB() {
        return tokenB;
    }

    public void setTokenB(String tokenB) {
        this.tokenB = tokenB;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
}
