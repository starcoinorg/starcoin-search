package org.starcoin.search.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.starcoin.search.bean.TransactionPayload;

public interface TransactionPayloadRepository extends JpaRepository<TransactionPayload, String> {
}
