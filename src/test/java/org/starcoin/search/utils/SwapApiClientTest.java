package org.starcoin.search.utils;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.starcoin.search.SearchApplication;
import org.starcoin.search.bean.SwapPoolInfo;
import org.starcoin.search.bean.SwapToken;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(
        classes = SearchApplication.class)
@TestPropertySource(
        locations = "classpath:application-integrationtest.properties")
class SwapApiClientTest {

    @Autowired
    private SwapApiClient swapApiClient;
    @Test
    void getTokens() throws IOException {
        List<SwapToken> swapTokenList = swapApiClient.getTokens("barnard");
        for(SwapToken token: swapTokenList) {
            System.out.println(token);
        }
    }

    @Test
    void getPoolInfo() throws IOException {
        List<SwapPoolInfo> poolInfoList = swapApiClient.getPoolInfo("barnard");
        for(SwapPoolInfo poolInfo: poolInfoList) {
            System.out.println(poolInfo);
        }
    }
}