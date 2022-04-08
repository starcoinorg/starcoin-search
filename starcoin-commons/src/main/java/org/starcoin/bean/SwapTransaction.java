package org.starcoin.bean;

import javax.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "swap_transaction")
public class SwapTransaction {
    @SequenceGenerator(name = "seq_txn_id", allocationSize = 1, initialValue = 1, sequenceName = "swap_transaction_swap_seq_seq")
    @GeneratedValue(generator = "seq_txn_id", strategy = GenerationType.SEQUENCE)
    @Column(name = "swap_seq")
    private long swapSeq;
    @Column(name = "total_value")
    private BigDecimal totalValue;
    @Column(name = "token_a")
    private String tokenA;
    @Column(name = "amount_a")
    private BigDecimal amountA;
    @Column(name = "token_b")
    private String tokenB;
    @Column(name = "amount_b")
    private BigDecimal amountB;
    @Column(name = "account")
    private String account;
    @Column(name = "ts")
    private long timestamp;
    @Column(name = "swap_type")
    private SwapType swapType;
    @Id
    @Column(name = "transaction_hash")
    private String transactionHash;

    public long getSwapSeq() {
        return swapSeq;
    }

    public void setSwapSeq(long swapSeq) {
        this.swapSeq = swapSeq;
    }

    public String getTransactionHash() {
        return transactionHash;
    }

    public void setTransactionHash(String transactionHash) {
        this.transactionHash = transactionHash;
    }

    public BigDecimal getTotalValue() {
        return totalValue;
    }

    public void setTotalValue(BigDecimal totalValue) {
        this.totalValue = totalValue;
    }

    public String getTokenA() {
        return tokenA;
    }

    public void setTokenA(String tokenA) {
        this.tokenA = tokenA;
    }

    public BigDecimal getAmountA() {
        return amountA;
    }

    public void setAmountA(BigDecimal amountA) {
        this.amountA = amountA;
    }

    public BigDecimal getAmountB() {
        return amountB;
    }

    public void setAmountB(BigDecimal amountB) {
        this.amountB = amountB;
    }

    public String getTokenB() {
        return tokenB;
    }

    public void setTokenB(String tokenB) {
        this.tokenB = tokenB;
    }


    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public SwapType getSwapType() {
        return swapType;
    }

    public void setSwapType(SwapType swapType) {
        this.swapType = swapType;
    }

    @Override
    public String toString() {
        return "SwapTransaction{" +
                "totalValue=" + totalValue +
                ", tokenA='" + tokenA + '\'' +
                ", amountA=" + amountA +
                ", tokenB='" + tokenB + '\'' +
                ", amountB=" + amountB +
                ", account='" + account + '\'' +
                ", timestamp=" + timestamp +
                ", swapType=" + swapType +
                ", transactionHash='" + transactionHash + '\'' +
                '}';
    }
}
