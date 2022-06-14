package org.starcoin.bean;

import javax.persistence.*;
import java.math.BigInteger;
import java.util.Date;

@Entity
@Table(name = "transfer_journal")
public class TransferJournalEntity {
    @Id
    @SequenceGenerator(name = "seq_transfer_journal_id", allocationSize = 1, initialValue = 1, sequenceName = "transfer_journal_transfer_id_seq")
    @GeneratedValue(generator = "seq_transfer_journal_id", strategy = GenerationType.SEQUENCE)
    @Column(name = "transfer_id")
    private long id;
    @Column(name = "address")
    private String address;
    @Column(name = "token")
    private String token;
    @Column(name = "amount")
    private BigInteger amount;
    @Column(name = "create_time")
    private Date createTime;

    public TransferJournalEntity(String address, String token, BigInteger amount, Date createTime) {
        this.address = address;
        this.token = token;
        this.amount = amount;
        this.createTime = createTime;
    }

    public TransferJournalEntity() {

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

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    @Override
    public String toString() {
        return "TransferJournalEntity{" +
                "transferId='" + id + '\'' +
                ", address='" + address + '\'' +
                ", token='" + token + '\'' +
                ", amount=" + amount +
                ", createTime=" + createTime +
                '}';
    }
}
