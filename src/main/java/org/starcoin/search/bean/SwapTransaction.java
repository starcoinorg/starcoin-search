package org.starcoin.search.bean;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.math.BigInteger;

@Entity
@Table(name = "swap_transaction")
public class SwapTransaction {

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
