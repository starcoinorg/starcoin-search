package org.starcoin.scan.service.vo;

/**
 * Starcoin DAG Inspector Edge info
 */
public class DIEdge {
    String fromBlockId;
    String toBlockId;
    Integer fromHeight;
    Integer toHeight;
    Integer toHeightGroupIndex;

    public String getFromBlockId() {
        return fromBlockId;
    }

    public void setFromBlockId(String fromBlockId) {
        this.fromBlockId = fromBlockId;
    }

    public String getToBlockId() {
        return toBlockId;
    }

    public void setToBlockId(String toBlockId) {
        this.toBlockId = toBlockId;
    }

    public Integer getFromHeight() {
        return fromHeight;
    }

    public void setFromHeight(Integer fromHeight) {
        this.fromHeight = fromHeight;
    }

    public Integer getToHeight() {
        return toHeight;
    }

    public void setToHeight(Integer toHeight) {
        this.toHeight = toHeight;
    }

    public Integer getToHeightGroupIndex() {
        return toHeightGroupIndex;
    }

    public void setToHeightGroupIndex(Integer toHeightGroupIndex) {
        this.toHeightGroupIndex = toHeightGroupIndex;
    }
}