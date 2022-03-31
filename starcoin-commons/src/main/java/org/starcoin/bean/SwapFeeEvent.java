package org.starcoin.bean;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "swap_fee_event")
public class SwapFeeEvent {
    @Id
    @SequenceGenerator(name = "seq_event_id", allocationSize = 1, initialValue = 1, sequenceName = "swap_fee_event_event_id_seq")
    @GeneratedValue(generator = "seq_event_id", strategy = GenerationType.SEQUENCE)
    @Column(name = "event_id")
    private long id;
    @Column(name = "token_first")
    private String tokenFirst;
    @Column(name = "token_second")
    private String tokenSecond;
    @Column(name = "swap_fee")
    private long swapFee;
    @Column(name = "fee_out")
    private long feeOut;
    @Column(name = "fee_addree")
    private String feeAddree;
    @Column(name = "signer")
    private String signer;
    @Column(name = "ts")
    private Date timestamp;
    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public static SwapFeeEvent fromJson(SwapFeeEventJson eventJson) {
        SwapFeeEvent swapFeeEvent = new SwapFeeEvent();
        swapFeeEvent.setTokenFirst(eventJson.getxTokenCode().uniform());
        swapFeeEvent.setTokenSecond(eventJson.getyTokenCode().uniform());
        swapFeeEvent.setSwapFee(eventJson.getSwapFee());
        swapFeeEvent.setFeeOut(eventJson.getFeeOut());
        swapFeeEvent.setFeeAddree(eventJson.getFeeAddree());
        swapFeeEvent.setSigner(eventJson.getSigner());
        swapFeeEvent.setTimestamp(new Date());
        return swapFeeEvent;
    }

    public PoolFeeStat toPoolFeeStat() {
        PoolFeeStat poolFeeStat = new PoolFeeStat(tokenFirst, tokenSecond, timestamp);
        poolFeeStat.setFeesAmount(BigDecimal.valueOf(feeOut));
        poolFeeStat.setFees(BigDecimal.valueOf(0));
        return poolFeeStat;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTokenFirst() {
        return tokenFirst;
    }

    public void setTokenFirst(String tokenFirst) {
        this.tokenFirst = tokenFirst;
    }

    public String getTokenSecond() {
        return tokenSecond;
    }

    public void setTokenSecond(String tokenSecond) {
        this.tokenSecond = tokenSecond;
    }

    public long getSwapFee() {
        return swapFee;
    }

    public void setSwapFee(long swapFee) {
        this.swapFee = swapFee;
    }

    public long getFeeOut() {
        return feeOut;
    }

    public void setFeeOut(long feeOut) {
        this.feeOut = feeOut;
    }

    public String getFeeAddree() {
        return feeAddree;
    }

    public void setFeeAddree(String feeAddree) {
        this.feeAddree = feeAddree;
    }

    public String getSigner() {
        return signer;
    }

    public void setSigner(String signer) {
        this.signer = signer;
    }
}
