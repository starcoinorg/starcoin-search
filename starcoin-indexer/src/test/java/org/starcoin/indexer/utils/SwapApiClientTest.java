package org.starcoin.indexer.utils;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.starcoin.indexer.SearchApplication;
import org.starcoin.bean.LiquidityPoolInfo;
import org.starcoin.bean.SwapToken;
import org.starcoin.utils.SwapApiClient;

import java.io.IOException;
import java.util.List;

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
        for (SwapToken token : swapTokenList) {
            System.out.println(token);
        }
    }

    @Test
    void getPoolInfo() throws IOException {
        List<LiquidityPoolInfo> poolInfoList = swapApiClient.getPoolInfo("barnard");
        for (LiquidityPoolInfo poolInfo : poolInfoList) {
            System.out.println(poolInfo);
        }
    }
}