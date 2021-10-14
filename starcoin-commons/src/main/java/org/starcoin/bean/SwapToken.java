package org.starcoin.bean;

import com.alibaba.fastjson.annotation.JSONField;

public class SwapToken {
    private String tokenId;
    @JSONField(name = "tokenStructType")
    private StructTag structTag;

    public String getTokenId() {
        return tokenId;
    }

    public void setTokenId(String tokenId) {
        this.tokenId = tokenId;
    }

    public StructTag getStructTag() {
        return structTag;
    }

    public void setStructTag(StructTag structTag) {
        this.structTag = structTag;
    }

    @Override
    public String toString() {
        return "SwapToken{" +
                "tokenId='" + tokenId + '\'' +
                ", structTag=" + structTag +
                '}';
    }
}
