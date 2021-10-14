package org.starcoin.indexer.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.novi.serde.DeserializationError;
import com.thetransactioncompany.jsonrpc2.client.JSONRPC2SessionException;
import org.elasticsearch.client.RestHighLevelClient;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.starcoin.api.StateRPCClient;
import org.starcoin.bean.Transaction;
import org.starcoin.bean.OracleTokenPair;
import org.starcoin.bean.SwapTransaction;
import org.starcoin.bean.TransferOffset;
import org.starcoin.constant.Constant;
import org.starcoin.indexer.service.SwapTxnService;
import org.starcoin.utils.SwapApiClient;
import org.starcoin.types.*;
import org.starcoin.utils.*;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TransactionPayloadHandle extends QuartzJobBean {

    private static final Logger logger = LoggerFactory.getLogger(TransactionPayloadHandle.class);
    private ObjectMapper objectMapper;
    private RestHighLevelClient client;
    private String index;

    @Value("${starcoin.network}")
    private String network;

    @Autowired
    private ElasticSearchHandler elasticSearchHandler;
    @Autowired
    private StateRPCClient stateRPCClient;
    @Autowired
    private SwapTxnService swapTxnService;
    @Autowired
    private SwapApiClient swapApiClient;

    @PostConstruct
    public void init() {
        objectMapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(StructTag.class, new StructTagDeserializer());
        module.addDeserializer(TypeTag.class, new TypeTagDeserializer());
        module.addDeserializer(ModuleId.class, new ModuleDeserializer());
        module.addDeserializer(ScriptFunction.class, new ScriptFunctionDeserializer());
        module.addDeserializer(TransactionPayload.class, new TransactionPayloadDeserializer());

        module.addSerializer(TransactionPayload.class, new TransactionPayloadSerializer());
        module.addSerializer(TypeTag.class, new TypeTagSerializer());
        module.addSerializer(StructTag.class, new StructTagSerializer());
        module.addSerializer(ScriptFunction.class, new ScriptFunctionSerializer());
        module.addSerializer(ModuleId.class, new ModuleIdSerializer());

        objectMapper.registerModule(module);
        //init client and index
        if (elasticSearchHandler != null) {
            client = elasticSearchHandler.getClient();
        }
        index = ServiceUtils.getIndex(network, Constant.PAYLOAD_INDEX);
    }

    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) {
        if (client == null) {
            init();
        }
        TransferOffset transactionPayloadRemoteOffset = ServiceUtils.getRemoteOffset(client, index);
        logger.info("handle txn payload: {}", transactionPayloadRemoteOffset);
        if (transactionPayloadRemoteOffset == null) {
            //init offset
            transactionPayloadRemoteOffset = new TransferOffset();
            transactionPayloadRemoteOffset.setTimestamp("0");
            ServiceUtils.setRemoteOffset(client, index, transactionPayloadRemoteOffset);
            logger.info("offset not init, init ok!");
        }
        try {
            List<Transaction> transactionList = elasticSearchHandler.getTransactionByTimestamp(transactionPayloadRemoteOffset.getTimestamp());
            if (!transactionList.isEmpty()) {
                List<SwapTransaction> swapTransactionList = new ArrayList<>();
                elasticSearchHandler.addUserTransactionToList(transactionList);
                elasticSearchHandler.bulkAddPayload(index, transactionList, objectMapper, swapTransactionList);
                //add es success and add swap txn
                if (!swapTransactionList.isEmpty()) {
                    Map<String, BigDecimal> tokenPriceMap = new HashMap<>();
                    for (SwapTransaction swapTransaction : swapTransactionList) {
                        List<String> tokenList = new ArrayList<>();
                        tokenList.add(swapTransaction.getTokenA());
                        tokenList.add(swapTransaction.getTokenB());
                        swapTransaction.setAmountA(ServiceUtils.divideScalingFactor(stateRPCClient, swapTransaction.getTokenA(), swapTransaction.getAmountA()));
                        swapTransaction.setAmountB(ServiceUtils.divideScalingFactor(stateRPCClient, swapTransaction.getTokenB(), swapTransaction.getAmountB()));
                        BigDecimal value = getTokenPrice(tokenPriceMap, swapTransaction.getTokenA(), swapTransaction.getAmountA(),
                                swapTransaction.getTokenB(), swapTransaction.getAmountB());
                        if (value != null) {
                            swapTransaction.setTotalValue(value);
                        } else {
                            //get oracle price
                            List<org.starcoin.bean.OracleTokenPair> oracleTokenPairs =
                                    swapApiClient.getProximatePriceRounds(network, tokenList, String.valueOf(swapTransaction.getTimestamp()));
                            if (!oracleTokenPairs.isEmpty()) {
                                OracleTokenPair oracleToken = oracleTokenPairs.get(0);
                                if (oracleToken != null) {
                                    BigDecimal price = new BigDecimal(oracleToken.getPrice());
                                    price = price.movePointLeft(oracleToken.getDecimals());
                                    tokenPriceMap.put(swapTransaction.getTokenA(), price);
                                    swapTransaction.setTotalValue(price.multiply(swapTransaction.getAmountA()));
                                } else {
                                    oracleToken = oracleTokenPairs.get(1);
                                    if (oracleToken != null) {
                                        BigDecimal price = new BigDecimal(oracleToken.getPrice());
                                        price = price.movePointLeft(oracleToken.getDecimals());
                                        tokenPriceMap.put(swapTransaction.getTokenB(), price);
                                        swapTransaction.setTotalValue(price.multiply(swapTransaction.getAmountB()));
                                    } else {
                                        logger.warn("get oracle price null: {}, {}, {}", swapTransaction.getTokenA(), swapTransaction.getTokenB(), swapTransaction.getTimestamp());
                                        swapTransaction.setTotalValue(new BigDecimal(0));
                                    }
                                }
                            }
                        }
                    }

                    swapTxnService.saveList(swapTransactionList);
                    logger.info("save swap txn ok: {}", swapTransactionList.size());
                }
                //update offset
                Transaction last = transactionList.get(transactionList.size() - 1);
                TransferOffset currentOffset = new TransferOffset();
                currentOffset.setTimestamp(String.valueOf(last.getTimestamp()));
                ServiceUtils.setRemoteOffset(client, index, currentOffset);
                logger.info("update payload ok: {}", currentOffset);
            } else {
                logger.warn("get txn_info null");
            }

        } catch (IOException | DeserializationError | JSONRPC2SessionException e) {
            logger.warn("handle transaction payload error:", e);
        }
    }

    private BigDecimal getTokenPrice(Map<String, BigDecimal> priceMap, String tokenA, BigDecimal amountA, String tokenB, BigDecimal amountB) {
        BigDecimal price = priceMap.get(tokenA);
        if (price != null) {
            return price.multiply(amountA);
        }
        price = priceMap.get(tokenB);
        if (price != null) {
            return price.multiply(amountB);
        }
        return null;
    }
}
