package org.starcoin.indexer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.starcoin.bean.HandleOffset;

public interface HandleOffsetRepository extends JpaRepository<HandleOffset, String> {
    HandleOffset getByOffsetId(String offsetId);
}
