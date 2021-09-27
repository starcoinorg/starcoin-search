package org.starcoin.search.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.starcoin.search.bean.SwapStat;

import java.util.Date;

public interface SwapStatRepository extends JpaRepository<SwapStat, Date> {
}
