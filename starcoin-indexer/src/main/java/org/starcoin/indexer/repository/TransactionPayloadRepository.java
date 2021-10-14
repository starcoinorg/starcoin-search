package org.starcoin.indexer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.starcoin.bean.TransactionPayload;

public interface TransactionPayloadRepository extends JpaRepository<TransactionPayload, String> {
}
