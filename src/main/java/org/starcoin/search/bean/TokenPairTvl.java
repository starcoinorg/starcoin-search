package org.starcoin.search.bean;

public class TokenPairTvl {

    private TokenTvlAmount x;

    private TokenTvlAmount y;

    public TokenPairTvl(TokenTvlAmount x, TokenTvlAmount y) {
        this.x = x;
        this.y = y;
    }

    public TokenTvlAmount getX() {
        return x;
    }

    public void setX(TokenTvlAmount x) {
        this.x = x;
    }

    public TokenTvlAmount getY() {
        return y;
    }

    public void setY(TokenTvlAmount y) {
        this.y = y;
    }
}
