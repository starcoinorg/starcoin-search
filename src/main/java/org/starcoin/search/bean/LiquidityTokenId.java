package org.starcoin.search.bean;

public class LiquidityTokenId{

    private String tokenXId;

    private String tokenYId;

    private String liquidityTokenAddress;

    public String getTokenXId() {
        return tokenXId;
    }

    public void setTokenXId(String tokenXId) {
        this.tokenXId = tokenXId;
    }

    public String getTokenYId() {
        return tokenYId;
    }

    public void setTokenYId(String tokenYId) {
        this.tokenYId = tokenYId;
    }

    public String getLiquidityTokenAddress() {
        return liquidityTokenAddress;
    }

    public void setLiquidityTokenAddress(String liquidityTokenAddress) {
        this.liquidityTokenAddress = liquidityTokenAddress;
    }

    @Override
    public String toString() {
        return "LiquidityTokenId{" +
                "tokenXId='" + tokenXId + '\'' +
                ", tokenYId='" + tokenYId + '\'' +
                ", liquidityTokenAddress='" + liquidityTokenAddress + '\'' +
                '}';
    }
}
