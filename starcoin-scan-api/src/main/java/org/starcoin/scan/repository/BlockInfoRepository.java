package org.starcoin.scan.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.starcoin.bean.BlockInfoEntity;

public interface BlockInfoRepository extends JpaRepository<BlockInfoEntity, String> {
}
