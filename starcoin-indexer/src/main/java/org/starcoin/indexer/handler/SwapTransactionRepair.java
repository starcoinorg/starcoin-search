package org.starcoin.indexer.handler;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.novi.serde.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.starcoin.api.StateRPCClient;
import org.starcoin.bean.*;
import org.starcoin.constant.StarcoinNetwork;
import org.starcoin.indexer.repository.TransactionPayloadRepository;
import org.starcoin.indexer.service.SwapTxnService;
import org.starcoin.utils.Hex;
import org.starcoin.utils.SwapApiClient;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SwapTransactionRepair {
    private static final Logger logger = LoggerFactory.getLogger(SwapTransactionRepair.class);
    @Value("${starcoin.network}")
    private String network;
    private StarcoinNetwork localNetwork;
    @Autowired
    private SwapApiClient swapApiClient;
    @Autowired
    private StateRPCClient stateRPCClient;
    @Autowired
    private SwapTxnService swapTxnService;

    @Autowired
    private TransactionPayloadRepository transactionPayloadRepository;

    @PostConstruct
    public void init() {
        //init network
        if (localNetwork == null) {
            localNetwork = StarcoinNetwork.fromValue(network);
        }
    }

    public void repair(int begin, int end) throws IOException {
        logger.info("swap transaction repair begin ..");
        //read from swap transaction list
        List<SwapTransaction> swapTransactionList = swapTxnService.getRemoveSwapTransactions();
        if(swapTransactionList == null || swapTransactionList.isEmpty()) {
            logger.info("remove transaction list is null");
            return;
        }
        //get token list
        Map<String, String> tokenInfos = new HashMap<>();
        List<String> tokens = new ArrayList<>();
        try {
            List<SwapToken> tokenList = swapApiClient.getTokens(localNetwork.getValue());
            for (SwapToken token: tokenList) {
                String tokenTag = token.getStructTag().toString();
                tokenInfos.put(token.getTokenId(), tokenTag);
                tokens.add(tokenTag);
            }
        } catch (IOException e) {
            logger.error("get token list error:", e);
        }

        int count = 0;
        for (SwapTransaction swapTransaction: swapTransactionList) {
            long seq = swapTransaction.getSwapSeq();
            if(seq< begin || seq > end) {
                continue;
            }
            logger.info("repair before: {}", swapTransaction);
            String txnId = swapTransaction.getTransactionHash();
            TransactionPayload transactionPayload = transactionPayloadRepository.findByTransactionHash(txnId);
            if(transactionPayload == null) {
                logger.warn("payload not exist: {}", txnId);
                continue;
            }
            String jsonVal = transactionPayload.getJson();
            JSONObject object = JSON.parseObject(jsonVal);
            JSONObject value = object.getJSONObject("value");
            String address = value.getJSONObject("module").getString("address");
            JSONArray jsonArray = value.getJSONArray("type_args");
            String tagA = jsonArray.getJSONObject(0).getJSONObject("value").getString("struct_tag_type");
            String tagB = jsonArray.getJSONObject(1).getJSONObject("value").getString("struct_tag_type");
            JSONArray args = value.getJSONArray("args");
            List<String> argsVal =  args.toJavaList(String.class);
            swapTransaction.setTokenA(tagA);
            swapTransaction.setTokenB(tagB);
            swapTransaction.setAmountA(new BigDecimal( ServiceUtils.deserializeU128(new Bytes(Hex.decode(argsVal.get(1))))));
            swapTransaction.setAmountB(new BigDecimal( ServiceUtils.deserializeU128(new Bytes(Hex.decode(argsVal.get(2))))));
            swapTransaction.setAmountA(ServiceUtils.divideScalingFactor(stateRPCClient, tagA, swapTransaction.getAmountA()));
            swapTransaction.setAmountB(ServiceUtils.divideScalingFactor(stateRPCClient, tagB, swapTransaction.getAmountB()));

            //get price
            long timestamp = swapTransaction.getTimestamp();
            Map<String, BigDecimal> tokenPrices = new HashMap<>();
            List<OracleTokenPair> pairs = swapApiClient.getProximatePriceRounds(localNetwork.getValue(), tokens, String.valueOf(timestamp));
            if (pairs != null && !pairs.isEmpty()) {
                BigDecimal tokenPrice;
                for (OracleTokenPair pair : pairs) {
                    if (pair != null) {
                        tokenPrice = new BigDecimal(pair.getPrice());
                        tokenPrice = tokenPrice.movePointLeft(pair.getDecimals());
                        String token = pair.getToken();
                        if (token != null) {
                            String longName = tokenInfos.get(token);
                            if (longName != null) {
                                tokenPrices.put(longName, tokenPrice);
                            } else {
                                logger.warn("to long name err: {}", token);
                            }
                        } else {
                            logger.warn("get token null from pair: {}", pair);
                        }
                    }
                }
            }

            //total value
            BigDecimal total = getTotalValue(swapTransaction, tokenPrices);
            if(total == null) {
                logger.warn("get swap transaction total null: {}", swapTransaction.getTransactionHash());
                swapTxnService.updateAmount(swapTransaction);
            }else {
                swapTransaction.setTotalValue(total);
                swapTxnService.updateAmountAndTotal(swapTransaction);
            }
            logger.info("update swap transaction : {}",  swapTransaction);
            count++;
        }
        logger.info("repair swap transaction ok: {}", count);
    }

    private BigDecimal getTotalValue(SwapTransaction swapTxn, Map<String, BigDecimal> priceMap) {
        BigDecimal priceA = priceMap.get(swapTxn.getTokenA());
        BigDecimal priceB = priceMap.get(swapTxn.getTokenB());
        boolean isSwap = SwapType.isSwap(swapTxn.getSwapType());
        if (isSwap) {
            if (priceA != null) {
                return priceA.multiply(swapTxn.getAmountA());
            } else if (priceB != null) {
                return priceB.multiply(swapTxn.getAmountB());
            }
        } else {
            BigDecimal total;
            BigDecimal two = new BigDecimal(2);
            if (priceA != null) {
                total = priceA.multiply(swapTxn.getAmountA());
                if (priceB != null) {
                    return total.add(priceB.multiply(swapTxn.getAmountB()));
                } else {
                    return total.multiply(two);
                }
            } else {
                if (priceB != null) {
                    return two.multiply(priceB.multiply(swapTxn.getAmountB()));
                }
            }
        }
        return null;
    }
}
