package org.starcoin.bean;

import com.alibaba.fastjson.annotation.JSONField;

public class TokenTransfer {
    @JSONField(name = "type_tag")
    private String typeTag;

    private long transfers;

    public String getTypeTag() {
        return typeTag;
    }

    public void setTypeTag(String typeTag) {
        this.typeTag = typeTag;
    }

    public long getTransfers() {
        return transfers;
    }

    public void setTransfers(long transfers) {
        this.transfers = transfers;
    }
}
