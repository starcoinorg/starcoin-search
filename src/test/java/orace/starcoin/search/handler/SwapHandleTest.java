package orace.starcoin.search.handler;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.starcoin.search.SearchApplication;
import org.starcoin.search.bean.SwapTransaction;
import org.starcoin.search.bean.SwapType;
import org.starcoin.search.handler.SwapHandle;
import org.starcoin.search.service.SwapTxnService;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Calendar;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest(
        classes = SearchApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(
        locations = "classpath:application-integrationtest.properties")
public class SwapHandleTest {

    @Autowired
    private SwapHandle swapHandle;

    @Autowired
    private SwapTxnService swapTxnService;

    static long getTimeStamp(int day) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH) + day, 0, 0, 0);
        return calendar.getTimeInMillis();
    }

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

}
