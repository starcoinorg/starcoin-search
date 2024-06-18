package org.starcoin.indexer.test;

import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.starcoin.indexer.IndexerApplication;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = IndexerApplication.class)
@TestPropertySource(locations = "classpath:application-integrationtest.properties")
public class IndexerLogicBaseTest {
}
