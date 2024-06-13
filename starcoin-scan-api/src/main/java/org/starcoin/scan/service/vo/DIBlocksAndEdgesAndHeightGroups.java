package org.starcoin.scan.service.vo;

import java.util.List;

/**
 * DAG Inspector query object
 */
public class DIBlocksAndEdgesAndHeightGroups {
    List<DIBlock> blocks;
    List<DIEdge> edges;
    List<DIHeightGroup> heightGroups;

    public List<DIBlock> getBlocks() {
        return blocks;
    }

    public void setBlocks(List<DIBlock> blocks) {
        this.blocks = blocks;
    }

    public List<DIEdge> getEdges() {
        return edges;
    }

    public void setEdges(List<DIEdge> edges) {
        this.edges = edges;
    }

    public List<DIHeightGroup> getHeightGroups() {
        return heightGroups;
    }

    public void setHeightGroups(List<DIHeightGroup> heightGroups) {
        this.heightGroups = heightGroups;
    }
}