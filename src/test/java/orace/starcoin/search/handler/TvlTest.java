package orace.starcoin.search.handler;

import com.thetransactioncompany.jsonrpc2.client.JSONRPC2SessionException;
import org.junit.Test;
import org.starcoin.api.ContractRPCClient;
import org.starcoin.bean.ContractCall;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class TvlTest {

    @Test
    public void  testTvl(){
        try {
            ContractRPCClient client = new ContractRPCClient(new URL("http://barnard1.seed.starcoin.org:9850"));

            ContractCall call = new ContractCall();

            call.setFunctionId("0xbd7e8be8fae9f60f2f5136433e36a091::TokenSwap::get_reserves");

            List<String> typeTags = new ArrayList<>();
            typeTags.add("0x00000000000000000000000000000001::STC::STC");
            typeTags.add("0xbd7e8be8fae9f60f2f5136433e36a091::Usdx::Usdx");

            call.setTypeArgs(typeTags);
            call.setArgs(new ArrayList<>());

            List result = client.call(call);
            System.out.println(result);

        } catch (JSONRPC2SessionException | MalformedURLException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void  testTvlService(){

    }

}
