package org.starcoin.bean;

public class LiquidityPoolId {

    private String poolAddress;

    private LiquidityTokenId liquidityTokenId;

    public String getPoolAddress() {
        return poolAddress;
    }

    public void setPoolAddress(String poolAddress) {
        this.poolAddress = poolAddress;
    }

    public LiquidityTokenId getLiquidityTokenId() {
        return liquidityTokenId;
    }

    public void setLiquidityTokenId(LiquidityTokenId liquidityTokenId) {
        this.liquidityTokenId = liquidityTokenId;
    }

    @Override
    public String toString() {
        return "LiquidityPoolId{" +
                "poolAddress='" + poolAddress + '\'' +
                ", liquidityTokenId=" + liquidityTokenId +
                '}';
    }
}
