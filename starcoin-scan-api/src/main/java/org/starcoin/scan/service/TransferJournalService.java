package org.starcoin.scan.service;

import org.springframework.stereotype.Service;
import org.starcoin.scan.repository.TokenVolumeDTO;
import org.starcoin.scan.repository.TransferJournalRepository;

import java.math.BigDecimal;

@Service
public class TransferJournalService extends BaseService {

    public BigDecimal getTokenVolume(String network, String token) {
       TransferJournalRepository repository = getTransferJournalRepository(network);
       if(repository != null) {
           TokenVolumeDTO tokenVolumeDTO = repository.getVolumeByToken(token);
           if(tokenVolumeDTO != null) {
               return tokenVolumeDTO.getVolume();
           }
       }
        return null;
    }
}
