package org.starcoin.bean;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "rate_limit")
public class RateLimit {
    @Id
    @SequenceGenerator(name = "seq_rate_limit_id", allocationSize = 1, initialValue = 1, sequenceName = "rate_limit_id_seq")
    @GeneratedValue(generator = "seq_rate_limit_id", strategy = GenerationType.SEQUENCE)
    @Column(name = "rl_id")
    private long id;
    @Column(name = "key_id")
    private long keyId;
    @Column(name = "rate_limit")
    private int rateLimit;
    @Column(name = "create_time")
    private Date createTime;
    @Column(name = "last_update")
    private Date lastUpdate;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getKeyId() {
        return keyId;
    }

    public void setKeyId(long keyId) {
        this.keyId = keyId;
    }

    public int getRateLimit() {
        return rateLimit;
    }

    public void setRateLimit(int rateLimit) {
        this.rateLimit = rateLimit;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(Date lastUpdate) {
        this.lastUpdate = lastUpdate;
    }
}
