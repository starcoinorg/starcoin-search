package org.starcoin.indexer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.starcoin.bean.PoolFeeStat;

import java.util.List;

public interface PoolFeeStatRepository extends JpaRepository<PoolFeeStat, String> {
    @Query(value = "select * from pool_fee_day_stat where (first_token_name=:token_x_name and second_token_name=:token_y_name) or (first_token_name=:token_y_name and second_token_name=:token_x_name) limit :count offset :offset", nativeQuery = true)
    List<PoolFeeStat> findAll(@Param("token_x_name") String tokenXName, @Param("token_y_name") String tokenYName,
                              @Param("offset") int offset,
                              @Param("count") int count);
}
