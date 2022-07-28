package org.starcoin.scan.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.starcoin.bean.BlockInfoEntity;
import org.starcoin.scan.repository.BlockInfoRepository;

import java.util.Optional;

@Service
public class BlockInfoService {
    @Autowired
    private BaseService baseService;

    public BlockInfoEntity getBlockInfoByHash(String network, String hash) {

        BlockInfoRepository repository = baseService.getBlockInfoRepository(network);
        if(repository != null) {
            Optional<BlockInfoEntity> entity = repository.findById(hash);
            if(entity.isPresent()) {
                return entity.get();
            }
        }
        return null;
    }
}
