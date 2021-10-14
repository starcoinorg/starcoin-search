package org.starcoin.indexer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.starcoin.bean.SwapStat;

import java.util.Date;

public interface SwapStatRepository extends JpaRepository<SwapStat, Date> {
}
