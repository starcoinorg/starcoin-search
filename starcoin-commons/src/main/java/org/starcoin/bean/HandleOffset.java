package org.starcoin.bean;

import javax.persistence.*;

@Entity
@Table(name = "handle_offset")
public class HandleOffset {
    @Id
    @Column(name = "offset_id")
    private String offsetId;
    @Column(name = "ts")
    private long timestamp;
    @Column(name = "offset_value")
    private long offset;

    public HandleOffset(String offsetId, long timestamp, long offset) {
        this.offsetId = offsetId;
        this.timestamp = timestamp;
        this.offset = offset;
    }

    public HandleOffset(String offsetId, long offset) {
        this.offsetId = offsetId;
        this.offset = offset;
    }

    public HandleOffset() {
    }

    public String getOffsetId() {
        return offsetId;
    }

    public void setOffsetId(String offsetId) {
        this.offsetId = offsetId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public long getOffset() {
        return offset;
    }

    public void setOffset(long offset) {
        this.offset = offset;
    }
}
