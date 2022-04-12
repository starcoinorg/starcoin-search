package org.starcoin.indexer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.starcoin.bean.SwapTransaction;
import org.starcoin.bean.TokenStat;

import java.util.Date;
import java.util.List;

public interface TokenStatRepository extends JpaRepository<TokenStat, String> {
    @Query(value = "select * from {h-domain}token_swap_day_stat where token_name =:token_name and ts =:start_time", nativeQuery = true)
    TokenStat findTokenStatById(@Param("start_time") Date startTime, @Param("token_name") String tokenName);

    @Query(value = "select * from {h-domain}token_swap_day_stat where ts =:start_time", nativeQuery = true)
    List<TokenStat> findTokenStatByDate(@Param("start_time") Date startTime);

    @Query(value = "select distinct token_name from token_swap_day_stat", nativeQuery = true)
    List<String> getAllToken();
}
