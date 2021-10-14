package org.starcoin.indexer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.starcoin.bean.SwapPoolStat;

public interface SwapPoolStatRepository extends JpaRepository<SwapPoolStat, String> {
}
