package org.starcoin.search.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.starcoin.search.bean.SwapTransaction;

import java.util.List;

public interface SwapTransactionRepository extends JpaRepository<SwapTransaction, String> {
    List<SwapTransaction> findAllByTransactionHash(String transactionHash);
}
