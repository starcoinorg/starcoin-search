package org.starcoin.bean;

import com.alibaba.fastjson.annotation.JSONField;

public class BlockInfo {
    @JSONField(name = "block_accumulator_info")
    private AccumulatorInfo blockAccumulatorInfo;
    @JSONField(name = "block_hash")
    private String blockHash;
    @JSONField(name = "total_difficulty")
    private String totalDifficulty;
    @JSONField(name = "txn_accumulator_info")
    private AccumulatorInfo txnAccumulatorInfo;

    public AccumulatorInfo getBlockAccumulatorInfo() {
        return blockAccumulatorInfo;
    }

    public void setBlockAccumulatorInfo(AccumulatorInfo blockAccumulatorInfo) {
        this.blockAccumulatorInfo = blockAccumulatorInfo;
    }

    public String getBlockHash() {
        return blockHash;
    }

    public void setBlockHash(String blockHash) {
        this.blockHash = blockHash;
    }

    public String getTotalDifficulty() {
        return totalDifficulty;
    }

    public void setTotalDifficulty(String totalDifficulty) {
        this.totalDifficulty = totalDifficulty;
    }

    public AccumulatorInfo getTxnAccumulatorInfo() {
        return txnAccumulatorInfo;
    }

    public void setTxnAccumulatorInfo(AccumulatorInfo txnAccumulatorInfo) {
        this.txnAccumulatorInfo = txnAccumulatorInfo;
    }

    @Override
    public String toString() {
        return "BlockInfo{" +
                "blockAccumulatorInfo=" + blockAccumulatorInfo +
                ", blockHash='" + blockHash + '\'' +
                ", totalDifficulty='" + totalDifficulty + '\'' +
                ", txnAccumulatorInfo=" + txnAccumulatorInfo +
                '}';
    }
}
