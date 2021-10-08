package org.starcoin.search.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.starcoin.search.bean.SwapTransaction;

import java.util.List;

public interface SwapTransactionRepository extends JpaRepository<SwapTransaction, String> {
    List<SwapTransaction> findAllByTransactionHash(String transactionHash);

    @Query(value = "select sum(total_value) as volume, sum(amount_a) as volumeAmount  from {h-domain}swap_transaction where token_a = :token "
            + " and swap_type > 0 and swap_type < 3 and (ts between :start_time and :end_time)", nativeQuery = true)
    TokenVolumeDTO getVolumeByTokenA(@Param("token") String tokenA, @Param("start_time") long startTime, @Param("end_time") long endTime);

    @Query(value = "select sum(total_value) as volume, sum(amount_b) as volumeAmount  from {h-domain}swap_transaction where token_b = :token "
            + "and swap_type > 0 and swap_type < 3 and (ts between :start_time and :end_time)", nativeQuery = true)
    TokenVolumeDTO getVolumeByTokenB(@Param("token") String tokenB, @Param("start_time") long startTime, @Param("end_time") long endTime);

    @Query(value = "select sum(total_value) as volume, sum(amount_a) as volumeAmount  from {h-domain}swap_transaction where token_a = :tokenA "
            + "and token_b = :tokenB "
            + " and swap_type > 0 and swap_type < 3 and (ts between :start_time and :end_time)", nativeQuery = true)
    TokenVolumeDTO getPoolVolumeA(@Param("tokenA") String tokenA, @Param("tokenB") String tokenB,
                                  @Param("start_time") long startTime, @Param("end_time") long endTime);

    @Query(value = "select sum(total_value) as volume, sum(amount_b) as volumeAmount  from {h-domain}swap_transaction where token_b = :tokenA "
            + "and token_a = :tokenB "
            + " and swap_type > 0 and swap_type < 3 and (ts between :start_time and :end_time)", nativeQuery = true)
    TokenVolumeDTO getPoolVolumeB(@Param("tokenA") String tokenA, @Param("tokenB") String tokenB,
                                  @Param("start_time") long startTime, @Param("end_time") long endTime);

}
