package org.starcoin.bean;

public class TransferOffset {
    private String timestamp;
    private int offset;

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
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
