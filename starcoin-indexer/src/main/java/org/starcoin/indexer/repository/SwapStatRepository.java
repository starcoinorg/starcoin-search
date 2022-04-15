package org.starcoin.indexer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.starcoin.bean.SwapStat;
import org.starcoin.bean.TokenStat;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;

public interface SwapStatRepository extends JpaRepository<SwapStat, Date> {
    @Query(value = "select * from {h-domain}swap_day_stat where stat_date =:start_time", nativeQuery = true)
    SwapStat findTokenStatByDate(@Param("start_time") Date startTime);

    @Modifying
    @Transactional
    @Query(value = "update {h-domain}swap_day_stat set tvl=:tvl " +
            " where  stat_date =:stat_date", nativeQuery = true)
    void updateTvl(@Param("stat_date") Date startTime,
                            @Param("tvl") BigDecimal tvl);
}
