package org.starcoin.search.bean;

import com.alibaba.fastjson.annotation.JSONField;

public class TokenPair {

    @JSONField(name = "token_first")
    private String tokenFirst;

    @JSONField(name = "token_second")
    private String tokenSecond;

    public String getTokenFirst() {
        return tokenFirst;
    }

    public String getTokenSecond() {
        return tokenSecond;
    }
}
