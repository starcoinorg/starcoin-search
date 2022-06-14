package org.starcoin.indexer.repository;

import java.math.BigInteger;

public interface MarketCapDTO {
    BigInteger getMarket();
    String getToken();
}
