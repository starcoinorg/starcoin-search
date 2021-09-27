package org.starcoin.search.repository;

import java.math.BigDecimal;
import java.math.BigInteger;

public interface TokenVolumeDTO {
    public BigDecimal getVolume();

    public BigInteger getVolumeAmount();
}
