package org.starcoin.scan.service.vo;

import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.starcoin.bean.*;

import java.util.ArrayList;
import java.util.List;

public class BlockVo extends Block {
    private Long daaScore;

    private Integer heightGroupIndex;

    List<String> mergeSetBlueIds;

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

    public List<String> getMergeSetBlueIds() {
        return mergeSetBlueIds;
    }

    public void setMergeSetBlueIds(List<String> mergeSetBlueIds) {
        this.mergeSetBlueIds = mergeSetBlueIds;
    }

    public static BlockVo from(Block block) {
        BlockVo blockVo = new BlockVo();

        blockVo.setBlockMetadata(block.getBlockMetadata());
        blockVo.setBody(block.getBody());
        blockVo.setUncles(block.getUncles());
        blockVo.setHeader(block.getHeader());
        blockVo.setRaw(block.getRaw());
        blockVo.setBody(block.getBody());

        blockVo.setDaaScore(0L);
        blockVo.setHeightGroupIndex(0);
        blockVo.setMergeSetBlueIds(new ArrayList<>());

        return blockVo;
    }
}
