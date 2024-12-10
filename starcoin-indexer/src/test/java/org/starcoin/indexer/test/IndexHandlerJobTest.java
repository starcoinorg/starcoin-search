package org.starcoin.indexer.test;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.starcoin.api.BlockRPCClient;
import org.starcoin.api.TransactionRPCClient;
import org.starcoin.indexer.handler.ElasticSearchHandler;
import org.starcoin.indexer.handler.LegacyMainIndexHandler;

public class IndexHandlerJobTest extends IndexerLogicBaseTest {

    @Value("${starcoin.indexer.bulk_size}")
    private long bulkSize;

    @Autowired
    private ElasticSearchHandler elasticSearchHandler;

    @Autowired
    private TransactionRPCClient transactionRPCClient;

    @Autowired
    private BlockRPCClient blockRPCClient;

    @Test
    public void testIndexerHandle() throws Exception {
        LegacyMainIndexHandler legacyMainIndexHandler = new LegacyMainIndexHandler(elasticSearchHandler, transactionRPCClient, blockRPCClient, bulkSize);
        legacyMainIndexHandler.initOffsetWith(2713240L, "0x4d58d276809bd061ba422a4699c90c790efc5dd1b6d40e8c2adb0b1cb98dfafd");
        legacyMainIndexHandler.execute();
    }
}
