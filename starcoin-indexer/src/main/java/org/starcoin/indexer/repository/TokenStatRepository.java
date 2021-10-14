package org.starcoin.indexer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.starcoin.bean.TokenStat;

public interface TokenStatRepository extends JpaRepository<TokenStat, String> {
}
