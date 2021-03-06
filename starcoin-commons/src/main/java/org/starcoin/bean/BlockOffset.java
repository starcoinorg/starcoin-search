package org.starcoin.bean;

import com.alibaba.fastjson.annotation.JSONField;

public class BlockOffset {
    @JSONField(name = "block_number")
    private long blockHeight;
    @JSONField(name = "block_hash")
    private String blockHash;

    public BlockOffset(long blockHeight, String blockHash) {
        this.blockHeight = blockHeight;
        this.blockHash = blockHash;
    }

    public long getBlockHeight() {
        return blockHeight;
    }

    public void setBlockHeight(long blockHeight) {
        this.blockHeight = blockHeight;
    }

    public String getBlockHash() {
        return blockHash;
    }

    public void setBlockHash(String blockHash) {
        this.blockHash = blockHash;
    }

    @Override
    public String toString() {
        return "Offset{" +
                "blockHeight=" + blockHeight +
                ", blockHash='" + blockHash + '\'' +
                '}';
    }
}
