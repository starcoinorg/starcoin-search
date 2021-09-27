package org.starcoin.search.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.starcoin.search.bean.SwapPoolStat;

public interface SwapPoolStatRepository extends JpaRepository<SwapPoolStat, String> {
}
