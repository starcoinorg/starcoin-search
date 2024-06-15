package org.starcoin.bean;

public class DagInspectorEdge {
    String fromBlockHash;
    String toBlockHash;
    Long fromHeight;
    Long toHeight;
    Integer fromHeightGroupIndex;
    Integer toHeightGroupIndex;

    public String getFromBlockHash() {
        return fromBlockHash;
    }

    public void setFromBlockHash(String fromBlockHash) {
        this.fromBlockHash = fromBlockHash;
    }

    public String getToBlockHash() {
        return toBlockHash;
    }

    public void setToBlockHash(String toBlockHash) {
        this.toBlockHash = toBlockHash;
    }

    public Long getFromHeight() {
        return fromHeight;
    }

    public void setFromHeight(Long fromHeight) {
        this.fromHeight = fromHeight;
    }

    public Long getToHeight() {
        return toHeight;
    }

    public void setToHeight(Long toHeight) {
        this.toHeight = toHeight;
    }

    public Integer getFromHeightGroupIndex() {
        return fromHeightGroupIndex;
    }

    public void setFromHeightGroupIndex(Integer fromHeightGroupIndex) {
        this.fromHeightGroupIndex = fromHeightGroupIndex;
    }

    public Integer getToHeightGroupIndex() {
        return toHeightGroupIndex;
    }

    public void setToHeightGroupIndex(Integer toHeightGroupIndex) {
        this.toHeightGroupIndex = toHeightGroupIndex;
    }
}
