package org.starcoin.scan.service.vo;

import org.starcoin.bean.DagInspectorBlock;
import org.starcoin.bean.DagInspectorEdge;
import org.starcoin.bean.DagInspectorHeightGroup;

import java.util.List;

/**
 * DAG Inspector query object
 */
public class DIBlocksAndEdgesAndHeightGroupsVo {
    List<DagInspectorBlock> blocks;
    List<DagInspectorEdge> edges;
    List<DagInspectorHeightGroup> heightGroups;

    public List<DagInspectorBlock> getBlocks() {
        return blocks;
    }

    public void setBlocks(List<DagInspectorBlock> blocks) {
        this.blocks = blocks;
    }

    public List<DagInspectorEdge> getEdges() {
        return edges;
    }

    public void setEdges(List<DagInspectorEdge> edges) {
        this.edges = edges;
    }

    public List<DagInspectorHeightGroup> getHeightGroups() {
        return heightGroups;
    }

    public void setHeightGroups(List<DagInspectorHeightGroup> heightGroups) {
        this.heightGroups = heightGroups;
    }

    @Override
    public String toString() {
        return "DIBlocksAndEdgesAndHeightGroupsVo{" +
                "blocks=" + blocks +
                ", edges=" + edges +
                ", heightGroups=" + heightGroups +
                '}';
    }
}