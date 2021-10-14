package org.starcoin.indexer.repository;

import java.math.BigDecimal;

public interface TokenVolumeDTO {
    BigDecimal getVolume();

    BigDecimal getVolumeAmount();
}
