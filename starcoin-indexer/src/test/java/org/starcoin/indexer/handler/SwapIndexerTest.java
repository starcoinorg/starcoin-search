package org.starcoin.indexer.handler;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.starcoin.bean.TokenTvl;
import org.starcoin.bean.TotalTvl;
import org.starcoin.constant.StarcoinNetwork;
import org.starcoin.indexer.IndexerApplication;
import org.starcoin.bean.SwapTransaction;
import org.starcoin.bean.SwapType;
import org.starcoin.indexer.service.SwapTxnService;
import org.starcoin.jsonrpc.client.JSONRPC2SessionException;
import org.starcoin.utils.SwapApiClient;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.List;

import static org.starcoin.utils.DateTimeUtils.getTimeStamp;

@RunWith(SpringRunner.class)
@SpringBootTest(
        classes = IndexerApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(
        locations = "classpath:application-integrationtest.properties")
class SwapHandleTest {

    @Autowired
    private SwapHandle swapHandle;

    @Autowired
    private SwapTxnService swapTxnService;

    @Autowired
    private SwapApiClient swapApiClient;

    @Test
    public void testSwapStats() {
        long endTs = getTimeStamp(0);
        long startTs = getTimeStamp(-1);

        swapHandle.swapStat(1632633811190L, 1632368803198L);
    }

    @Test
    public void testSwapTxn() {
        SwapTransaction swapTransaction = new SwapTransaction();
        swapTransaction.setTransactionHash("0x23c60406800d7693532e4b5ca59d7c8737ad4204092a025a7f1d37633eb58a1d");
        swapTransaction.setSwapType(SwapType.SwapExactTokenForToken);
        swapTransaction.setAccount("0x161d3fd393ebf6becb9c78c9b41ad1b9");
        swapTransaction.setTokenA("0x00000000000000000000000000000001::STC::STC");
        swapTransaction.setAmountA(new BigDecimal("1000"));
        swapTransaction.setTokenB("0x9350502a3af6c617e9a42fa9e306a385::BX_USDT::BX_USDT");
        swapTransaction.setAmountB(new BigDecimal("20"));
        swapTransaction.setTimestamp(1617194769000L);
        swapTransaction.setTotalValue(new BigDecimal(2000));
        swapTxnService.save(swapTransaction);
    }

    @Test
    public void testGetALLSwapTxn() {
        List<SwapTransaction> swapTransactionList = swapTxnService.getAll();
        for (SwapTransaction swapTransaction : swapTransactionList) {
            System.out.println(swapTransaction);
        }
    }

    @org.junit.jupiter.api.Test
    public void getTvl() throws IOException {
       List<TokenTvl> tokenTvls = swapApiClient.getTokenTvl(StarcoinNetwork.fromValue("main.0727").getValue());
        for (TokenTvl tvl : tokenTvls) {
            System.out.println(tvl);
        }
    }

    @org.junit.jupiter.api.Test
    void testSwapTVL() throws JSONRPC2SessionException {
       TotalTvl totalTvl = swapHandle.getTvls();
       System.out.println(totalTvl);
    }
}
