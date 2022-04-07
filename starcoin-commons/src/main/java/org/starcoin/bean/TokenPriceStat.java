package org.starcoin.bean;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "token_price_stat")
public class TokenPriceStat {
    @EmbeddedId
    TokenPriceStatId tokenPriceStatId;

    @Column(name = "price")
    private BigDecimal price;
    @Column(name = "max_price")
    private BigDecimal maxPrice;
    @Column(name = "min_price")
    private BigDecimal minPrice;
    @Column(name = "rate")
    private BigDecimal rate;

    public TokenPriceStat(String token, Date date, BigDecimal price, BigDecimal maxPrice, BigDecimal minPrice) {
        this.tokenPriceStatId = new TokenPriceStatId(token,date);
        this.price = price;
        this.maxPrice = maxPrice;
        this.minPrice = minPrice;
    }

    public TokenPriceStat() {
    }

    public BigDecimal getPrice() {
        return price;
    }

    public BigDecimal getMaxPrice() {
        return maxPrice;
    }

    public void setMaxPrice(BigDecimal maxPrice) {
        this.maxPrice = maxPrice;
    }

    public BigDecimal getMinPrice() {
        return minPrice;
    }

    public void setMinPrice(BigDecimal minPrice) {
        this.minPrice = minPrice;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getRate() {
        return rate;
    }

    public void setRate(BigDecimal rate) {
        this.rate = rate;
    }

    public String getToken() {
        return tokenPriceStatId.getToken();
    }

    public Date getTimestamp() {
        return tokenPriceStatId.getTimestamp();
    }

    @Override
    public String toString() {
        return "TokenPriceStat{" +
                "tokenPriceStatId=" + tokenPriceStatId +
                ", price=" + price +
                ", maxPrice=" + maxPrice +
                ", minPrice=" + minPrice +
                ", rate=" + rate +
                '}';
    }
}

@Embeddable
class TokenPriceStatId implements Serializable {
    @Column(name = "token_name")
    private String token;
    @Column(name = "ts")
    private Date timestamp;

    public TokenPriceStatId(String token, Date timestamp) {
        this.token = token;
        this.timestamp = timestamp;
    }

    public TokenPriceStatId() {
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "TokenPriceStatId{" +
                "token='" + token + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
