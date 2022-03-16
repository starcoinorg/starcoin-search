package org.starcoin.scan.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.starcoin.scan.entity.BlockInfo;
import org.starcoin.scan.repository.BlockInfoRepository;

@Service
public class BlockInfoService {
    @Autowired
    private BaseService baseService;

    public BlockInfo getBlockInfoByHash(String network, String hash) {

        BlockInfoRepository repository = baseService.getBlockInfoRepository(network);
        if(repository != null) {
            return repository.getById(hash);
        }
        return null;
    }
}