package org.starcoin.indexer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.starcoin.bean.SwapPoolStat;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.List;

public interface SwapPoolStatRepository extends JpaRepository<SwapPoolStat, String> {
    @Query(value = "select * from {h-domain}pool_swap_day_stat where first_token_name =:first_token_name and second_token_name=:second_token_name and ts =:start_time", nativeQuery = true)
    SwapPoolStat findSwapPoolStatById(@Param("first_token_name") String tokenA, @Param("second_token_name") String tokenB, @Param("start_time") Date startTime);

    @Query(value = "select * from {h-domain}pool_swap_day_stat where ts =:start_time", nativeQuery = true)
    List<SwapPoolStat> findSwapPoolStatByDate(@Param("start_time") Date startTime);

    @Modifying
    @Transactional
    @Query(value = "update {h-domain}pool_swap_day_stat set tvl_a=:tvl_a, tvl_a_amount=:tvl_a_amount, tvl_b=:tvl_b, tvl_b_amount=:tvl_b_amount" +
            " where first_token_name =:first_token_name and second_token_name=:second_token_name and ts =:start_time", nativeQuery = true)
    void updateTvlAndAmount(@Param("first_token_name") String tokenA,
                          @Param("second_token_name") String tokenB,
                          @Param("start_time") Date startTime,
                          @Param("tvl_a") BigDecimal tvlA,
                          @Param("tvl_a_amount") BigInteger tvlAAmount,
                          @Param("tvl_b") BigDecimal tvlB,
                          @Param("tvl_b_amount") BigInteger tvlBAmount);
}
