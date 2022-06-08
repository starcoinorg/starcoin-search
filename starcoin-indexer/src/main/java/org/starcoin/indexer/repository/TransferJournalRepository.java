package org.starcoin.indexer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.starcoin.bean.TransferJournalEntity;

public interface TransferJournalRepository extends JpaRepository<TransferJournalEntity, String> {
}
