package org.starcoin.search.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.starcoin.search.bean.TokenStat;

public interface TokenStatRepository extends JpaRepository<TokenStat, String> {
}
