package org.starcoin.bean;

import com.alibaba.fastjson.annotation.JSONField;

import javax.persistence.*;
import java.math.BigInteger;
import java.util.Date;

@Entity
@Table(name = "address_holder")
public class AddressHolderEntity {
    @Id
    @SequenceGenerator(name = "seq_holder_id", allocationSize = 1, initialValue = 1, sequenceName = "address_holder_holder_id_seq")
    @GeneratedValue(generator = "seq_holder_id", strategy = GenerationType.SEQUENCE)
    @Column(name = "holder_id")
    private long id;
    @Column(name = "address")
    private String address;
    @Column(name = "token")
    @JSONField(name = "type_tag")
    private String token;
    @Column(name = "amount")
    private BigInteger amount;
    @Column(name = "update_time")
    private Date updateTime;

    public AddressHolderEntity(String address, String token) {
        this.address = address;
        this.token = token;
    }

    public AddressHolderEntity() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public BigInteger getAmount() {
        return amount;
    }

    public void setAmount(BigInteger amount) {
        this.amount = amount;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }
}
