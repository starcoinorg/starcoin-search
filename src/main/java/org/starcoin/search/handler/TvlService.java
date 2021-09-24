package org.starcoin.search.handler;

import com.thetransactioncompany.jsonrpc2.client.JSONRPC2SessionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.starcoin.api.ContractRPCClient;
import org.starcoin.bean.ContractCall;
import org.starcoin.search.bean.TokenPairTvl;
import org.starcoin.search.bean.TokenTvlAmount;

import java.math.BigDecimal;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class TvlService {

    private ContractRPCClient client;

    private static final Logger logger = LoggerFactory.getLogger(TvlService.class);


    public TvlService(URL url)  {
        client = new ContractRPCClient(url);
    }

    public TokenPairTvl getTokenPairTvl(String tokenX, String tokenY){
        try {
            ContractCall call = new ContractCall();

            call.setFunctionId("0xbd7e8be8fae9f60f2f5136433e36a091::TokenSwap::get_reserves");

            List<String> typeTags = new ArrayList<>();
            typeTags.add(tokenX);
            typeTags.add(tokenY);

            call.setTypeArgs(typeTags);
            call.setArgs(new ArrayList<>());

            List result = client.call(call);
            if (result.size() > 1) {
                long x = (Long) result.get(0);
                long y = (Long) result.get(1);

                return new TokenPairTvl(new TokenTvlAmount(tokenX,BigDecimal.valueOf(x)),new TokenTvlAmount(tokenY,BigDecimal.valueOf(y)));
            }
        }catch (JSONRPC2SessionException  e) {
            logger.warn("call contract function failed",e);
        }
        return null;
    }

}
