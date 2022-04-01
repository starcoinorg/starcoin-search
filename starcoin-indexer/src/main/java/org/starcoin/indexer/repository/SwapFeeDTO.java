package org.starcoin.indexer.repository;

import java.util.Date;

public interface SwapFeeDTO {
    long getSwapFee();
    String getTokenFirst();
    String getTokenSecond();
    Date getTs();
}
