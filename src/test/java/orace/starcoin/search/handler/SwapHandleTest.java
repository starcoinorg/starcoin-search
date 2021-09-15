package orace.starcoin.search.handler;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.starcoin.search.SearchApplication;
import org.starcoin.search.handler.SwapHandle;

import java.util.Calendar;

@RunWith(SpringRunner.class)
@SpringBootTest(
        classes = SearchApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(
        locations = "classpath:application-integrationtest.properties")
public class SwapHandleTest {

    @Autowired
    private SwapHandle swapHandle;

    @Test
    public void testSwapStats() {
        long endTs = getTimeStamp(0);
        long startTs = getTimeStamp(-1);

        swapHandle.volumeStats(startTs,endTs);
    }

    static long getTimeStamp(int day) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH) + day, 0, 0, 0);
        return calendar.getTimeInMillis();
    }

}
