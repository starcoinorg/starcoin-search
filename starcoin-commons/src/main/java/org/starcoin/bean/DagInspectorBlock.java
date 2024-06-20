package org.starcoin.bean;

import java.util.List;

public class DagInspectorBlock {
    String blockHash;
    Long timestamp;
    List<String> parentIds;
    Long height;
    Long daaScore;
    Integer heightGroupIndex;
    String selectedParentHash;
    String color;
    Boolean isInVirtualSelectedParentChain;
    List<String> mergeSetRedIds;
    List<String> mergeSetBlueIds;

    public String getBlockHash() {
        return blockHash;
    }

    public void setBlockHash(String blockHash) {
        this.blockHash = blockHash;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public List<String> getParentIds() {
        return parentIds;
    }

    public void setParentIds(List<String> parentIds) {
        this.parentIds = parentIds;
    }

    public Long getHeight() {
        return height;
    }

    public void setHeight(Long height) {
        this.height = height;
    }

    public Long getDaaScore() {
        return daaScore;
    }

    public void setDaaScore(Long daaScore) {
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

    public List<String> getMergeSetRedIds() {
        return mergeSetRedIds;
    }

    public void setMergeSetRedIds(List<String> mergeSetRedIds) {
        this.mergeSetRedIds = mergeSetRedIds;
    }

    public List<String> getMergeSetBlueIds() {
        return mergeSetBlueIds;
    }

    public void setMergeSetBlueIds(List<String> mergeSetBlueIds) {
        this.mergeSetBlueIds = mergeSetBlueIds;
    }

    @Override
    public String toString() {
        return "DagInspectorBlock{" +
                "blockHash='" + blockHash + '\'' +
                ", timestamp=" + timestamp +
                ", parentIds=" + parentIds +
                ", height=" + height +
                ", daaScore=" + daaScore +
                ", heightGroupIndex=" + heightGroupIndex +
                ", selectedParentHash='" + selectedParentHash + '\'' +
                ", color='" + color + '\'' +
                ", isInVirtualSelectedParentChain=" + isInVirtualSelectedParentChain +
                ", mergeSetRedIds=" + mergeSetRedIds +
                ", mergeSetBlueIds=" + mergeSetBlueIds +
                '}';
    }
}
