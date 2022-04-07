package org.starcoin.bean;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "token_price_day")
public class TokenPrice {
    @EmbeddedId
    TokenPriceId tokenPriceId;

    @Column(name = "price")
    private BigDecimal price;

    public TokenPrice(String tokenName, long date, BigDecimal price) {
        TokenPriceId priceId = new TokenPriceId(tokenName, date);
        this.tokenPriceId = priceId;
        this.price = price;
    }

    public TokenPrice() {
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getToken() {
        return tokenPriceId.getToken();
    }

    public long getTimestamp() {
        return tokenPriceId.getTimestamp();
    }
}

@Embeddable
class TokenPriceId implements Serializable {
    @Column(name = "token_name")
    private String token;
    @Column(name = "ts")
    private long timestamp;

    public TokenPriceId(String token, long timestamp) {
        this.token = token;
        this.timestamp = timestamp;
    }

    public TokenPriceId() {
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}