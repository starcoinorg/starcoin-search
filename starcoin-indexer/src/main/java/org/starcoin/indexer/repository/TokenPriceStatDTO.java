package org.starcoin.indexer.repository;

import java.math.BigDecimal;

public interface TokenPriceStatDTO {
    BigDecimal getPrice();
    BigDecimal getMaxPrice();
    BigDecimal getMinPrice();
}
