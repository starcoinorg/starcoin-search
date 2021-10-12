package org.starcoin.search.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.starcoin.search.bean.TransactionPayload;
import org.starcoin.search.repository.TransactionPayloadRepository;

import java.util.List;

@Service
public class TransactionPayloadService {
    @Autowired
    private TransactionPayloadRepository transactionPayloadRepository;

    public void savePayload(List<TransactionPayload> transactionPayloadList) {
        transactionPayloadRepository.saveAllAndFlush(transactionPayloadList);
    }
}
