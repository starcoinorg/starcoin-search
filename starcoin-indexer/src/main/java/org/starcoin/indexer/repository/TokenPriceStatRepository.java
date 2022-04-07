package org.starcoin.indexer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.starcoin.bean.TokenPriceStat;

public interface TokenPriceStatRepository extends JpaRepository<TokenPriceStat, String> {
}
