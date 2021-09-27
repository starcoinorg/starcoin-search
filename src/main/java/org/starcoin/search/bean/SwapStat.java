package org.starcoin.search.bean;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;

@Entity
@Table(name = "swap_day_stat")
public class SwapStat {
    @Id
    @Column(name = "stat_date")
    private Date statDate;
    @Column(name = "volume_amount")
    private BigInteger volumeAmount;
    @Column(name = "volume")
    private BigDecimal volume;
    @Column(name = "tvl")
    private BigDecimal tvl;
    @Column(name = "tvl_amount")
    private BigInteger tvlAmount;

    public Date getStatDate() {
        return statDate;
    }

    public void setStatDate(Date statDate) {
        this.statDate = statDate;
    }

    public BigInteger getVolumeAmount() {
        return volumeAmount;
    }

    public void setVolumeAmount(BigInteger volumeAmount) {
        this.volumeAmount = volumeAmount;
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

    public BigInteger getTvlAmount() {
        return tvlAmount;
    }

    public void setTvlAmount(BigInteger tvlAmount) {
        this.tvlAmount = tvlAmount;
    }
}
