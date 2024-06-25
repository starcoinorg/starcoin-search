package org.starcoin.scan.service.vo;

import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.starcoin.bean.*;

import java.util.ArrayList;
import java.util.List;

public class BlockVo extends Block {

    @JSONField(name = "daa_score")
    @JsonProperty("daa_score")
    private Long daaScore;

    @JSONField(name = "heightgroup_index")
    @JsonProperty("heightgroup_index")
    private Integer heightgroupIndex;

    @JSONField(name = "merged_blueset")
    @JsonProperty("merged_blueset")
    List<String> mergedBlueset;

    public Long getDaaScore() {
        return daaScore;
    }

    public void setDaaScore(Long daaScore) {
        this.daaScore = daaScore;
    }

    public Integer getHeightgroupIndex() {
        return heightgroupIndex;
    }

    public void setHeightgroupIndex(Integer heightgroupIndex) {
        this.heightgroupIndex = heightgroupIndex;
    }

    public List<String> getMergedBlueset() {
        return mergedBlueset;
    }

    public void setMergedBlueset(List<String> mergedBlueset) {
        this.mergedBlueset = mergedBlueset;
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
        blockVo.setHeightgroupIndex(0);
        blockVo.setMergedBlueset(new ArrayList<>());

        return blockVo;
    }
}
