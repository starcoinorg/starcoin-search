package org.starcoin.indexer.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.starcoin.bean.TransactionPayload;
import org.starcoin.indexer.repository.TransactionPayloadRepository;

import java.util.List;

@Service
public class TransactionPayloadService {
    @Autowired
    private TransactionPayloadRepository transactionPayloadRepository;

    public void savePayload(List<TransactionPayload> transactionPayloadList) {
        transactionPayloadRepository.saveAll(transactionPayloadList);
        transactionPayloadRepository.flush();
    }
}
