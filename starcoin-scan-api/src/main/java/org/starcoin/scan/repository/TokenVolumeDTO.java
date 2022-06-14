package org.starcoin.scan.repository;

import java.math.BigDecimal;

public interface TokenVolumeDTO {
    BigDecimal getVolume();
    String getToken();
}
