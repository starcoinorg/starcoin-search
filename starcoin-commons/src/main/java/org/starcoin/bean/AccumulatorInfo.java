package org.starcoin.bean;

import com.alibaba.fastjson.annotation.JSONField;

import java.util.Arrays;

public class AccumulatorInfo {
    @JSONField(name = "accumulator_root")
    private String accumulatorRoot;
    @JSONField(name = "frozen_subtree_roots")
    private String[] frozenSubtreeRoot;
    @JSONField(name = "num_leaves")
    private long numLeaves;
    @JSONField(name = "num_nodes")
    private long numNodes;

    public String getAccumulatorRoot() {
        return accumulatorRoot;
    }

    public void setAccumulatorRoot(String accumulatorRoot) {
        this.accumulatorRoot = accumulatorRoot;
    }

    public String[] getFrozenSubtreeRoot() {
        return frozenSubtreeRoot;
    }

    public void setFrozenSubtreeRoot(String[] frozenSubtreeRoot) {
        this.frozenSubtreeRoot = frozenSubtreeRoot;
    }

    public long getNumLeaves() {
        return numLeaves;
    }

    public void setNumLeaves(long numLeaves) {
        this.numLeaves = numLeaves;
    }

    public long getNumNodes() {
        return numNodes;
    }

    public void setNumNodes(long numNodes) {
        this.numNodes = numNodes;
    }

    @Override
    public String toString() {
        return "AccumulatorInfo{" +
                "accumulatorRoot='" + accumulatorRoot + '\'' +
                ", frozenSubtreeRoot=" + Arrays.toString(frozenSubtreeRoot) +
                ", numLeaves=" + numLeaves +
                ", numNodes=" + numNodes +
                '}';
    }
}
