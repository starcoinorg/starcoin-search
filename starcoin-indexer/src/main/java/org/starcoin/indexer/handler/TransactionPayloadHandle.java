package org.starcoin.indexer.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.novi.serde.DeserializationError;
import org.elasticsearch.client.RestHighLevelClient;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.starcoin.api.StateRPCClient;
import org.starcoin.bean.*;
import org.starcoin.bean.Transaction;
import org.starcoin.constant.Constant;
import org.starcoin.constant.StarcoinNetwork;
import org.starcoin.indexer.service.SwapTxnService;
import org.starcoin.jsonrpc.client.JSONRPC2SessionException;
import org.starcoin.types.*;
import org.starcoin.types.StructTag;
import org.starcoin.types.TransactionPayload;
import org.starcoin.types.TypeTag;
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
    private StarcoinNetwork localNetwork;
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
        //init network
        if (localNetwork == null) {
            localNetwork = StarcoinNetwork.fromValue(network);
        }
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
            transactionPayloadRemoteOffset.setOffset(0);
            ServiceUtils.setRemoteOffset(client, index, transactionPayloadRemoteOffset);
            logger.info("offset not init, init ok!");
        }
        try {
            List<Transaction> transactionList = elasticSearchHandler.getTransactionByGlobalIndex(transactionPayloadRemoteOffset.getOffset());
            if (!transactionList.isEmpty()) {
                List<SwapTransaction> swapTransactionList = new ArrayList<>();
                elasticSearchHandler.addUserTransactionToList(transactionList);
                long globalIndex = elasticSearchHandler.bulkAddPayload(index, transactionList, objectMapper, swapTransactionList);
                //add es success and add swap txn
                if (!swapTransactionList.isEmpty()) {
                    Map<String, BigDecimal> tokenPriceMap = new HashMap<>();
                    for (SwapTransaction swapTransaction : swapTransactionList) {
                        List<String> tokenList = new ArrayList<>();
                        tokenList.add(swapTransaction.getTokenA());
                        tokenList.add(swapTransaction.getTokenB());
                        swapTransaction.setAmountA(ServiceUtils.divideScalingFactor(stateRPCClient, swapTransaction.getTokenA(), swapTransaction.getAmountA()));
                        swapTransaction.setAmountB(ServiceUtils.divideScalingFactor(stateRPCClient, swapTransaction.getTokenB(), swapTransaction.getAmountB()));
                        boolean isSwap = SwapType.isSwap(swapTransaction.getSwapType());
                        BigDecimal value = getTotalValue(tokenPriceMap, swapTransaction.getTokenA(), swapTransaction.getAmountA(),
                                swapTransaction.getTokenB(), swapTransaction.getAmountB(), isSwap);
                        if (value != null) {
                            swapTransaction.setTotalValue(value);
                        } else {
                            int retry = 3;
                            while (retry > 0) {
                                //get oracle price
                                logger.info("token price not cache, load from oracle: {}, {}", swapTransaction.getTokenA(), swapTransaction.getTokenB());
                                long priceTime = swapTransaction.getTimestamp() - 300000 * (6 - retry);
                                List<org.starcoin.bean.OracleTokenPair> oracleTokenPairs =
                                        swapApiClient.getProximatePriceRounds(localNetwork.getValue(), tokenList, String.valueOf(priceTime));
                                if (oracleTokenPairs != null && !oracleTokenPairs.isEmpty()) {
                                    BigDecimal priceA = null;
                                    OracleTokenPair oracleTokenA = oracleTokenPairs.get(0);
                                    if (oracleTokenA != null) {
                                        priceA = new BigDecimal(oracleTokenA.getPrice());
                                        priceA = priceA.movePointLeft(oracleTokenA.getDecimals());
                                        tokenPriceMap.put(swapTransaction.getTokenA(), priceA);
                                        logger.info("get oracle price1 ok: {}", oracleTokenA);
                                    }
                                    // get tokenB price
                                    BigDecimal priceB = null;
                                    OracleTokenPair oracleTokenB = oracleTokenPairs.get(1);
                                    if (oracleTokenB != null) {
                                        priceB = new BigDecimal(oracleTokenB.getPrice());
                                        priceB = priceB.movePointLeft(oracleTokenB.getDecimals());
                                        tokenPriceMap.put(swapTransaction.getTokenB(), priceB);
                                        logger.info("get oracle price2 ok: {}", oracleTokenB);
                                    }
                                    BigDecimal zero = new BigDecimal(0);
                                    if (isSwap) {
                                        if (priceA != null && priceA.compareTo(zero) == 1) {
                                            swapTransaction.setTotalValue(priceA.multiply(swapTransaction.getAmountA()));
                                            break;
                                        }
                                        if (priceB != null && priceB.compareTo(zero) == 1) {
                                            swapTransaction.setTotalValue(priceB.multiply(swapTransaction.getAmountB()));
                                            break;
                                        }
                                        logger.warn("get oracle price null: {}, {}, {}", swapTransaction.getTokenA(), swapTransaction.getTokenB(), priceTime);
                                        retry--;
                                    } else {
                                        // add or remove
                                        boolean isExistB = priceB != null && priceB.compareTo(zero) == 1;
                                        if (priceA != null && priceA.compareTo(zero) == 1) {
                                            BigDecimal valueA = priceA.multiply(swapTransaction.getAmountA());
                                            if (isExistB) {
                                                swapTransaction.setTotalValue(priceB.multiply(swapTransaction.getAmountB()).add(valueA));
                                            } else {
                                                swapTransaction.setTotalValue(valueA.multiply(new BigDecimal(2)));
                                            }
                                            break;
                                        } else {
                                            if (isExistB) {
                                                swapTransaction.setTotalValue(priceB.multiply(swapTransaction.getAmountB()).multiply(new BigDecimal(2)));
                                                break;
                                            } else {
                                                logger.warn("get oracle price null: {}, {}, {}", swapTransaction.getTokenA(), swapTransaction.getTokenB(), priceTime);
                                                retry--;
                                            }
                                        }
                                    }
                                } else {
                                    logger.warn("getProximatePriceRounds null: {}, {}, {}", swapTransaction.getTokenA(), swapTransaction.getTokenB(), priceTime);
                                    retry--;
                                }
                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }

                    try {
                        swapTxnService.saveList(swapTransactionList);
                        logger.info("save swap txn ok: {}", swapTransactionList.size());
                    } catch (Exception e) {
                       logger.error("save swap err:", e);
                    }

                }
                //update offset
                Transaction last = transactionList.get(transactionList.size() - 1);
                TransferOffset currentOffset = new TransferOffset();
                currentOffset.setTimestamp(String.valueOf(last.getTimestamp()));
                currentOffset.setOffset(globalIndex);
                ServiceUtils.setRemoteOffset(client, index, currentOffset);
                logger.info("update payload ok: {}", currentOffset);
            } else {
                logger.warn("get txn_info null");
            }

        } catch (IOException | DeserializationError | JSONRPC2SessionException e) {
            logger.warn("handle transaction payload error:", e);
        }
    }

    private BigDecimal getTotalValue(Map<String, BigDecimal> priceMap, String tokenA, BigDecimal amountA, String tokenB,
                                     BigDecimal amountB, boolean isSwap) {
        BigDecimal priceA = priceMap.get(tokenA);
        BigDecimal priceB = priceMap.get(tokenB);
        if (isSwap) {
            if (priceA != null) {
                return priceA.multiply(amountA);
            } else if (priceB != null) {
                return priceB.multiply(amountB);
            }
        } else {
            BigDecimal total;
            BigDecimal two = new BigDecimal(2);
            if (priceA != null) {
                total = priceA.multiply(amountA);
                if (priceB != null) {
                    return total.add(priceB.multiply(amountB));
                } else {
                    return total.multiply(two);
                }
            } else {
                if (priceB != null) {
                    return two.multiply(priceB.multiply(amountB));
                }
            }
        }
        return null;
    }
}
