package org.starcoin.indexer.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.starcoin.bean.TransferJournalEntity;
import org.starcoin.indexer.repository.TransferJournalRepository;

@Service
public class TransferJournalService {
    @Autowired
    private TransferJournalRepository transferJournalRepository;

    public void save(TransferJournalEntity transferJournal) {
        transferJournalRepository.save(transferJournal);
    }
}
