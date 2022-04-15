package org.starcoin.bean;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "swap_day_stat")
public class SwapStat {
    @Id
    @Column(name = "stat_date")
    private Date statDate;
    @Column(name = "volume")
    private BigDecimal volume;
    @Column(name = "tvl")
    private BigDecimal tvl;
    public SwapStat(Date date) {
        this.statDate = date;
    }

    public SwapStat() {
    }

    public Date getStatDate() {
        return statDate;
    }

    public void setStatDate(Date statDate) {
        this.statDate = statDate;
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

    @Override
    public String toString() {
        return "SwapStat{" +
                "statDate=" + statDate +
                ", volume=" + volume +
                ", tvl=" + tvl +
                '}';
    }
}
