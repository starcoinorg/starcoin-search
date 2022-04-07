package org.starcoin.indexer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.starcoin.bean.TokenPrice;
import org.starcoin.bean.TokenPriceStat;

public interface TokenPriceRepository extends JpaRepository<TokenPrice, String> {
    @Query(value = "select max(price) as maxPrice, avg(price) as price, min(price) as minPrice from {h-domain}token_price_day where token_name = :token "
            + " and (ts between :start_time and :end_time)", nativeQuery = true)
    TokenPriceStatDTO getPriceByToken(@Param("token") String token, @Param("start_time") long startTime, @Param("end_time") long endTime);
}
