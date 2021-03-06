package org.starcoin.indexer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.starcoin.bean.PoolFeeStat;
import org.starcoin.bean.SwapFeeEvent;

import java.sql.Date;
import java.util.List;

public interface SwapFeeEventRepository extends JpaRepository<SwapFeeEvent, String> {
    @Query(value = "select sum(swap_fee) as fees_amount, token_first as first_token_name, token_second as second_token_name," +
        " ts  from {h-domain}swap_fee_event where ts =:stat_date and token_first=:token_first and token_second=:token_second  " +
        "group by token_first, token_second, ts", nativeQuery = true)
    PoolFeeStat sumByPoolName(@Param("token_first") String tokenFirst, @Param("token_second") String tokenSecond,@Param("stat_date") String statDate);

    @Query(value = "select sum(swap_fee) as swapFee, token_first as tokenFirst, token_second as tokenSecond, ts from {h-domain}swap_fee_event where ts =:stat_date " +
            "group by token_first, token_second, ts", nativeQuery = true)
    List<SwapFeeDTO> sumPoolFeeList(@Param("stat_date") Date statDate);
}
