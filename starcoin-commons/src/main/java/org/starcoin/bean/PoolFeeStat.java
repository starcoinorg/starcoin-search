package org.starcoin.bean;

import com.alibaba.fastjson.annotation.JSONField;
import org.starcoin.utils.TokenUtils;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "pool_fee_day_stat")
public class PoolFeeStat {

    @EmbeddedId
    @JSONField(serialize = false)
    PoolStatId id;
    @Column(name = "fees_amount")
    @JSONField(name = "fees_amount")
    private BigDecimal feesAmount;
    @Column(name = "fees")
    private BigDecimal fees;
    @JSONField(name = "pool_name")
    @Transient
    private String poolName;
    @JSONField(name = "timestamp")
    @Transient
    private Date timestamp;

    public PoolFeeStat(String tokenA, String tokenB, Date date) {
        PoolStatId id = new PoolStatId(tokenA, tokenB, date);
        this.id = id;
    }

    public PoolFeeStat() {
    }

    public BigDecimal getFeesAmount() {
        return feesAmount;
    }

    public void setFeesAmount(BigDecimal feesAmount) {
        this.feesAmount = feesAmount;
    }

    public BigDecimal getFees() {
        return fees;
    }

    public void setFees(BigDecimal fees) {
        this.fees = fees;
    }

    public void setPoolName(String poolName) {
        this.poolName = poolName;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public PoolStatId getId() {
        return id;
    }

    public void setId(PoolStatId id) {
        this.id = id;
    }

    public String getPoolName() {
        return TokenUtils.toShort(id.getTokenA()) + "/" + TokenUtils.toShort(id.getTokenB());
    }

    public Date getTimestamp() {
        return id.getTimestamp();
    }

}

