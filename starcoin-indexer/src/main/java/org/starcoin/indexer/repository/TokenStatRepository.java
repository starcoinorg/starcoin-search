package org.starcoin.indexer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.starcoin.bean.SwapTransaction;
import org.starcoin.bean.TokenStat;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.List;

public interface TokenStatRepository extends JpaRepository<TokenStat, String> {
    @Query(value = "select * from {h-domain}token_swap_day_stat where token_name =:token_name and ts =:start_time", nativeQuery = true)
    TokenStat findTokenStatById(@Param("start_time") Date startTime, @Param("token_name") String tokenName);

    @Query(value = "select * from {h-domain}token_swap_day_stat where ts =:start_time", nativeQuery = true)
    List<TokenStat> findTokenStatByDate(@Param("start_time") Date startTime);

    @Query(value = "select distinct token_name from token_swap_day_stat", nativeQuery = true)
    List<String> getAllToken();

    @Modifying
    @Transactional
    @Query(value = "update {h-domain}token_swap_day_stat set tvl=:tvl, tvl_amount=:tvl_amount" +
            " where token_name =:token_name and ts =:start_time", nativeQuery = true)
    void updateTvlAndAmount(@Param("token_name") String tokenA,
                            @Param("start_time") Date startTime,
                            @Param("tvl") BigDecimal tvl,
                            @Param("tvl_amount") BigInteger tvlAmount);
}
