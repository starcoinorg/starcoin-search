package org.starcoin.search.bean;

import org.starcoin.bean.Tvl;

import java.util.Map;

public class TotalTvl {

    private Map<String, Tvl> tokenTvlMap;

    private Map<String, TokenPairTvl> tokenPairTvlMap;

    public TotalTvl(Map<String, Tvl> tokenTvlMap, Map<String, TokenPairTvl> tokenPairTvlMap) {
        this.tokenTvlMap = tokenTvlMap;
        this.tokenPairTvlMap = tokenPairTvlMap;
    }

    public Map<String, Tvl> getTokenTvlMap() {
        return tokenTvlMap;
    }

    public void setTokenTvlMap(Map<String, Tvl> tokenTvlMap) {
        this.tokenTvlMap = tokenTvlMap;
    }

    public Map<String, TokenPairTvl> getTokenPairTvlMap() {
        return tokenPairTvlMap;
    }

    public void setTokenPairTvlMap(Map<String, TokenPairTvl> tokenPairTvlMap) {
        this.tokenPairTvlMap = tokenPairTvlMap;
    }
}
