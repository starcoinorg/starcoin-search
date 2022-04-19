package org.starcoin.bean;

public class TransferOffset {
    private String timestamp;
    private long offset;

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public long getOffset() {
        return offset;
    }

    public void setOffset(long offset) {
        this.offset = offset;
    }

    @Override
    public String toString() {
        return "TransferOffset{" +
                "timestamp=" + timestamp +
                ", offset=" + offset +
                '}';
    }
}
