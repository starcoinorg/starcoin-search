package org.starcoin.indexer.test;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.starcoin.indexer.IndexerApplication;

@SpringBootTest(classes = IndexerApplication.class)
@TestPropertySource(locations = "classpath:application-integrationtest.properties")
public class IndexerLogicBaseTest {
}
