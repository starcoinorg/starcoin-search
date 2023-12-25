package org.starcoin.indexer.handler;

import org.junit.Test;
import org.starcoin.api.ContractRPCClient;
import org.starcoin.api.StateRPCClient;
import org.starcoin.bean.ContractCall;
import org.starcoin.jsonrpc.client.JSONRPC2SessionException;


import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TvlTest {

    @Test
    public void testTvl() {
        try {
            ContractRPCClient client = new ContractRPCClient(new URL("http://barnard1.seed.starcoin.org:9850"));

            ContractCall call = new ContractCall();

            call.setFunctionId("0xbd7e8be8fae9f60f2f5136433e36a091::TokenSwap::get_reserves");

            List<String> typeTags = new ArrayList<>();
            typeTags.add("0x00000000000000000000000000000001::STC::STC");
            typeTags.add("0xbd7e8be8fae9f60f2f5136433e36a091::Usdx::Usdx");

            call.setTypeArgs(typeTags);
            call.setArgs(new ArrayList<>());

            List<Object> result = client.call(call);
            System.out.println(result);

        } catch (JSONRPC2SessionException | MalformedURLException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void TestGetTokenReserve() throws MalformedURLException, JSONRPC2SessionException {
        StateRPCClient client = new StateRPCClient(new URL("http://main.seed.starcoin.org:9850"));
      Map<String[], Long[]> poolReserves =  ServiceUtils.getTokenReserveFromState(client, "0x8c109349c6bd91411d6bc962e080c4a3", "0x23cd65c679f1e64aba2b9684b995349f362fb9775f40bc503c0c065490260208");
        for (String[] key: poolReserves.keySet()) {
            System.out.println(key[0] + "," + key[1]);
            Long[] value = poolReserves.get(key);
            System.out.println(value[0] + "," + value[1]);
        }
    }
}
