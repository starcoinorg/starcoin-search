package org.starcoin.search.bean;


public class TokenPoolStat {
    private TokenStat xStats;

    private TokenStat yStats;

    public TokenPoolStat(TokenStat xStats, TokenStat yStats) {
        this.xStats = xStats;
        this.yStats = yStats;
    }

    public TokenStat getxStats() {
        return xStats;
    }

    public void setxStats(TokenStat xStats) {
        this.xStats = xStats;
    }

    public TokenStat getyStats() {
        return yStats;
    }

    public void setyStats(TokenStat yStats) {
        this.yStats = yStats;
    }

    public void add(TokenPoolStat tokenPoolStat){
        xStats.add(tokenPoolStat.xStats);
        yStats.add(tokenPoolStat.yStats);
    }
}
