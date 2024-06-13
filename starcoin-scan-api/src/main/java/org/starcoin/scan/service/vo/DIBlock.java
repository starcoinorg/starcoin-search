package org.starcoin.scan.service.vo;

import java.util.Vector;

/**
 * Starcoin DAG Inspector Block info
 */
public class DIBlock {
    String blockHash;
    Integer timestamp;
    Vector<String> parentIds;
    Long height;
    Integer daaScore;
    Integer heightGroupIndex;
    String selectedParentHash;
    String color;
    Boolean isInVirtualSelectedParentChain;
    Vector<String> mergeSetRedIds;
    Vector<String> mergeSetBlueIds;

    public String getBlockHash() {
        return blockHash;
    }

    public void setBlockHash(String blockHash) {
        this.blockHash = blockHash;
    }

    public Integer getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Integer timestamp) {
        this.timestamp = timestamp;
    }

    public Vector<String> getParentIds() {
        return parentIds;
    }

    public void setParentIds(Vector<String> parentIds) {
        this.parentIds = parentIds;
    }

    public Long getHeight() {
        return height;
    }

    public void setHeight(Long height) {
        this.height = height;
    }

    public Integer getDaaScore() {
        return daaScore;
    }

    public void setDaaScore(Integer daaScore) {
        this.daaScore = daaScore;
    }

    public Integer getHeightGroupIndex() {
        return heightGroupIndex;
    }

    public void setHeightGroupIndex(Integer heightGroupIndex) {
        this.heightGroupIndex = heightGroupIndex;
    }

    public String getSelectedParentHash() {
        return selectedParentHash;
    }

    public void setSelectedParentHash(String selectedParentHash) {
        this.selectedParentHash = selectedParentHash;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public Boolean getInVirtualSelectedParentChain() {
        return isInVirtualSelectedParentChain;
    }

    public void setInVirtualSelectedParentChain(Boolean inVirtualSelectedParentChain) {
        isInVirtualSelectedParentChain = inVirtualSelectedParentChain;
    }

    public Vector<String> getMergeSetRedIds() {
        return mergeSetRedIds;
    }

    public void setMergeSetRedIds(Vector<String> mergeSetRedIds) {
        this.mergeSetRedIds = mergeSetRedIds;
    }

    public Vector<String> getMergeSetBlueIds() {
        return mergeSetBlueIds;
    }

    public void setMergeSetBlueIds(Vector<String> mergeSetBlueIds) {
        this.mergeSetBlueIds = mergeSetBlueIds;
    }
}