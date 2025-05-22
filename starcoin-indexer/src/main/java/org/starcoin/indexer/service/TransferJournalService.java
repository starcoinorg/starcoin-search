package org.starcoin.indexer.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.starcoin.bean.TransferJournalEntity;
import org.starcoin.indexer.repository.TransferJournalRepository;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
public class TransferJournalService {
    private static final Logger logger = LoggerFactory.getLogger(TransferJournalService.class);
    private static final int MAX_RETRIES = 3;
    private static final int BASE_DELAY_MS = 100;
    private static final Random random = new Random();

    @Autowired
    private TransferJournalRepository transferJournalRepository;

    public void save(TransferJournalEntity transferJournal) {
        int retryCount = 0;
        while (retryCount < MAX_RETRIES) {
            try {
                logger.info("Saving transfer journal entity: {}", transferJournal);
                transferJournalRepository.save(transferJournal);
                return;
            } catch (DataIntegrityViolationException e) {
                retryCount++;
                if (retryCount >= MAX_RETRIES) {
                    logger.error("Failed to save transfer journal after {} retries: {}", MAX_RETRIES, transferJournal, e);
                    throw e;
                }

                // Calculate delay with exponential backoff and jitter
                int delayMs = BASE_DELAY_MS * (1 << retryCount) + random.nextInt(BASE_DELAY_MS);
                logger.warn("Retry {} of {}: Primary key conflict detected, waiting {} ms before retry",
                        retryCount, MAX_RETRIES, delayMs);

                try {
                    TimeUnit.MILLISECONDS.sleep(delayMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted while waiting to retry save operation", ie);
                }
            }
        }
    }
}
