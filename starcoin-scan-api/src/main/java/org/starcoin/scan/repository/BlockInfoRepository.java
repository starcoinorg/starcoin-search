package org.starcoin.scan.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.starcoin.scan.entity.BlockInfo;

public interface BlockInfoRepository extends JpaRepository<BlockInfo, String> {
}
