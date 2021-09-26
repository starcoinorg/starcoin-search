package org.starcoin.search.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.starcoin.bean.OracleTokenPair;
import org.starcoin.search.SearchApplication;
import org.starcoin.search.bean.LiquidityPoolInfo;

import java.io.IOException;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest(
        classes = SearchApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(
        locations = "classpath:application-integrationtest.properties")
class OracleClientTest {

    @Autowired
    private OracleClient client;

    @BeforeEach
    void setUp() {
    }
    @Test
    void getOracleTokenPair() throws IOException {
        List<OracleTokenPair> pairList = client.getOracleTokenPair("main");
        for(OracleTokenPair pair: pairList) {
            System.out.println(pair);
        }
    }

    @Test
    void getProximatePriceRound() throws IOException {
       OracleTokenPair pair = client.getProximatePriceRound("main", "BTC_USD", "1632384134000");
        System.out.println(pair);
    }

    @Test
    void testGetPoolInfo() throws IOException {
        List<LiquidityPoolInfo> infoList = client.getPoolInfo("barnard");
        System.out.println(infoList);
    }

}