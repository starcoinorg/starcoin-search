package org.starcoin.search.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.starcoin.search.bean.SwapTransaction;
import org.starcoin.search.repository.SwapTransactionRepository;

import java.util.List;

@Service
public class SwapTxnService {
    @Autowired
    private SwapTransactionRepository swapTransactionRepository;

    public void save(SwapTransaction swapTransaction) {
        swapTransactionRepository.save(swapTransaction);
    }

    public List<SwapTransaction> getAll() {
        return swapTransactionRepository.findAll();
    }

    public void saveList(List<SwapTransaction> swapTransactionList) {
        swapTransactionRepository.saveAllAndFlush(swapTransactionList);
    }

}
