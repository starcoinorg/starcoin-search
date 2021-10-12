package org.starcoin.search.repository;

import java.math.BigDecimal;

public interface TokenVolumeDTO {
    BigDecimal getVolume();

    BigDecimal getVolumeAmount();
}
