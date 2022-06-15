package org.starcoin.indexer.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.starcoin.bean.TransferJournalEntity;
import org.starcoin.indexer.repository.TransferJournalRepository;

@Service
public class TransferJournalService {
    private static final Logger logger = LoggerFactory.getLogger(TransferJournalService.class);
    @Autowired
    private TransferJournalRepository transferJournalRepository;

    public void save(TransferJournalEntity transferJournal) {
        logger.info("save transfer journal entity: {}", transferJournal);
        transferJournalRepository.save(transferJournal);
    }
}
