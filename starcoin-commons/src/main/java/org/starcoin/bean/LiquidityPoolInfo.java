package org.starcoin.bean;

import java.math.BigDecimal;
import java.math.BigInteger;

public class LiquidityPoolInfo {

    private LiquidityPoolId liquidityPoolId;
    private String description;
    private int sequenceNumber;
    private long totalLiquidity;
    private BigInteger tokenXReserve;
    private BigInteger tokenYReserve;
    private BigDecimal tokenXReserveInUsd;
    private BigDecimal tokenYReserveInUsd;
    private boolean deactived;
    private String createdBy;
    private String updatedBy;
    private long createdAt;
    private long updatedAt;
    private int version;

    public LiquidityPoolId getLiquidityPoolId() {
        return liquidityPoolId;
    }

    public void setLiquidityPoolId(LiquidityPoolId liquidityPoolId) {
        this.liquidityPoolId = liquidityPoolId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(int sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public long getTotalLiquidity() {
        return totalLiquidity;
    }

    public void setTotalLiquidity(long totalLiquidity) {
        this.totalLiquidity = totalLiquidity;
    }

    public BigInteger getTokenXReserve() {
        return tokenXReserve;
    }

    public void setTokenXReserve(BigInteger tokenXReserve) {
        this.tokenXReserve = tokenXReserve;
    }

    public BigInteger getTokenYReserve() {
        return tokenYReserve;
    }

    public void setTokenYReserve(BigInteger tokenYReserve) {
        this.tokenYReserve = tokenYReserve;
    }

    public BigDecimal getTokenXReserveInUsd() {
        return tokenXReserveInUsd;
    }

    public void setTokenXReserveInUsd(BigDecimal tokenXReserveInUsd) {
        this.tokenXReserveInUsd = tokenXReserveInUsd;
    }

    public BigDecimal getTokenYReserveInUsd() {
        return tokenYReserveInUsd;
    }

    public void setTokenYReserveInUsd(BigDecimal tokenYReserveInUsd) {
        this.tokenYReserveInUsd = tokenYReserveInUsd;
    }

    public boolean isDeactived() {
        return deactived;
    }

    public void setDeactived(boolean deactived) {
        this.deactived = deactived;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "LiquidityPoolInfo{" +
                "liquidityPoolId=" + liquidityPoolId +
                ", description='" + description + '\'' +
                ", sequenceNumber=" + sequenceNumber +
                ", totalLiquidity=" + totalLiquidity +
                ", tokenXReserve=" + tokenXReserve +
                ", tokenYReserve=" + tokenYReserve +
                ", tokenXReserveInUsd=" + tokenXReserveInUsd +
                ", tokenYReserveInUsd=" + tokenYReserveInUsd +
                ", deactived=" + deactived +
                ", createdBy='" + createdBy + '\'' +
                ", updatedBy='" + updatedBy + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", version=" + version +
                '}';
    }
}
