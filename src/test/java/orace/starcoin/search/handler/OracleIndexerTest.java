package orace.starcoin.search.handler;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.starcoin.search.SearchApplication;
import org.starcoin.search.handler.OracleTokenPriceService;


@RunWith(SpringRunner.class)
@SpringBootTest(
        classes = SearchApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(
        locations = "classpath:application-integrationtest.properties")
@Ignore
public class OracleIndexerTest {

    @Autowired
    private OracleTokenPriceService oracleTokenPriceService;

    @Test
    @Ignore
    public void testFetchAndStoreOracleTokenPrice() throws JobExecutionException {
        oracleTokenPriceService.fetchAndStoreOracleTokenPrice();
    }
}
