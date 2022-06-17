package org.starcoin.bean;


import com.alibaba.fastjson.annotation.JSONField;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "block_info")
public class BlockInfoEntity {
    @Id
    @Column(name = "block_hash")
    private String blockHash;
    @Column(name = "block_number")
    @JSONField(serialize =false)
    private long blockNumber;
    @Column(name = "total_difficulty")
    @JSONField(serialize =false)
    private String totalDifficulty;
    @Column(name = "block_accumulator_info")
    @JSONField(serialize =false)
    private String blockAccumulatorInfo;
    @Column(name = "txn_accumulator_info")
    @JSONField(serialize =false)
    private String txnAccumulatorInfo;

    public String getBlockHash() {
        return blockHash;
    }

    public void setBlockHash(String blockHash) {
        this.blockHash = blockHash;
    }

    public long getBlockNumber() {
        return blockNumber;
    }

    public void setBlockNumber(long blockNumber) {
        this.blockNumber = blockNumber;
    }

    public String getTotalDifficulty() {
        return totalDifficulty;
    }

    public void setTotalDifficulty(String totalDifficulty) {
        this.totalDifficulty = totalDifficulty;
    }

    public String getBlockAccumulatorInfo() {
        return blockAccumulatorInfo;
    }

    public void setBlockAccumulatorInfo(String blockAccumulatorInfo) {
        this.blockAccumulatorInfo = blockAccumulatorInfo;
    }

    public String getTxnAccumulatorInfo() {
        return txnAccumulatorInfo;
    }

    public void setTxnAccumulatorInfo(String txnAccumulatorInfo) {
        this.txnAccumulatorInfo = txnAccumulatorInfo;
    }

    @Override
    public String toString() {
        return "BlockInfo{" +
                "blockHash='" + blockHash + '\'' +
                ", blockNumber=" + blockNumber +
                ", totalDifficulty='" + totalDifficulty + '\'' +
                ", blockAccumulatorInfo='" + blockAccumulatorInfo + '\'' +
                ", txnAccumulatorInfo='" + txnAccumulatorInfo + '\'' +
                '}';
    }
}
