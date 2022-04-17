package org.starcoin.indexer.handler;

import com.thetransactioncompany.jsonrpc2.client.JSONRPC2SessionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.starcoin.api.BlockRPCClient;
import org.starcoin.api.StateRPCClient;
import org.starcoin.bean.*;
import org.starcoin.constant.StarcoinNetwork;
import org.starcoin.indexer.service.SwapPoolStatService;
import org.starcoin.indexer.service.SwapStatService;
import org.starcoin.indexer.service.TokenStatService;
import org.starcoin.utils.SwapApiClient;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.starcoin.utils.DateTimeUtils.getWholeDatTime;

@Service
public class UpdateTVLHandle {
    private static final Logger logger = LoggerFactory.getLogger(UpdateTVLHandle.class);
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    @Value("${swap.contract.address}")
    private String contractAddress;
    @Value("${starcoin.network}")
    private String network;
    private StarcoinNetwork localNetwork;
    @Autowired
    private BlockRPCClient blockRPCClient;
    @Autowired
    private StateRPCClient stateRPCClient;
    @Autowired
    private SwapApiClient swapApiClient;
    @Autowired
    private SwapPoolStatService swapPoolStatService;
    @Autowired
    private TokenStatService tokenStatService;
    @Autowired
    private SwapStatService swapStatService;

    @PostConstruct
    public void init() {
        //init network
        if (localNetwork == null) {
            localNetwork = StarcoinNetwork.fromValue(network);
        }
    }

    public void handle(long beginBlockNumber) {
        //get block from chain
        try {
           Block block = blockRPCClient.getBlockByHeight(beginBlockNumber);
           if(block != null) {
               String stateRoot = block.getHeader().getStateRoot();
               long timestamp = block.getHeader().getTimestamp();
               logger.info("block num: {}, state root: {}", beginBlockNumber, stateRoot);
               Map<String[], Long[]> poolReserves = ServiceUtils.getTokenReserveFromState(stateRPCClient, contractAddress, stateRoot);
               Map<String, TokenTvl> tokenTvlMap = new HashMap<>();
               long wholeDateTime = getWholeDatTime(timestamp);
               Date statDate = new Date(wholeDateTime);
               SwapStat swapStat = new SwapStat(statDate);
               //update pool tvl
               for (String[] key: poolReserves.keySet()) {
                   logger.info("handle pool : {} / {}", key[1], key[0]);
                   //get token tvl
                   TokenTvl tokenTvlA = tokenTvlMap.get(key[1]);
                   TokenTvl tokenTvlB = tokenTvlMap.get(key[0]);
                   Long[] value = poolReserves.get(key);
                   if(value[0] == 0 || value[1]== 0) {
                       continue;
                   }
                   logger.info("handle pool amount : {} / {}", value[1], value[0]);
                   SwapPoolStat poolStat = new SwapPoolStat(key[1], key[0], wholeDateTime);
                   //set amount
                   BigDecimal amountA = ServiceUtils.divideScalingFactor(stateRPCClient, key[1], new BigDecimal(value[1]));
                   poolStat.setTvlAAmount(amountA.toBigInteger());
                   BigDecimal amountB = ServiceUtils.divideScalingFactor(stateRPCClient, key[0], new BigDecimal(value[0]));
                   poolStat.setTvlBAmount(amountB.toBigInteger());

                   //get price of tokens
                   List<OracleTokenPair> pairs = swapApiClient.getProximatePriceRounds(localNetwork.getValue(), Arrays.asList(key), String.valueOf(timestamp));
                   if (pairs != null && !pairs.isEmpty()) {
                       for (OracleTokenPair pair: pairs) {
                           if(pair != null) {
                               BigDecimal price = new BigDecimal(pair.getPrice());
                               price = price.movePointLeft(pair.getDecimals());
                               String token = pair.getToken();
                               if(token != null) {
                                   if(key[1].contains(token)) {
                                       //set tvl_a
                                       poolStat.setTvlA(price.multiply(amountA));
                                       //set token tvl and amount
                                       if(tokenTvlA == null) {
                                           tokenTvlA = new TokenTvl(key[1], amountA.toBigInteger(), poolStat.getTvlA());
                                       }else {
                                           //accumulate last result
                                           tokenTvlA.setTvlAmount(tokenTvlA.getTvlAmount().add(amountA.toBigInteger()));
                                           tokenTvlA.setTvl(tokenTvlA.getTvl().add(poolStat.getTvlA()));
                                       }
                                       tokenTvlMap.put(key[1], tokenTvlA);
                                   }
                                   if(key[0].contains(token)) {
                                       poolStat.setTvlB(price.multiply(amountB));
                                       if(tokenTvlB == null) {
                                           tokenTvlB = new TokenTvl(key[0], amountB.toBigInteger(), poolStat.getTvlB());
                                       }else {
                                           //accumulate last result
                                           tokenTvlB.setTvlAmount(tokenTvlB.getTvlAmount().add(amountB.toBigInteger()));
                                           tokenTvlB.setTvl(tokenTvlB.getTvl().add(poolStat.getTvlB()));
                                       }
                                       tokenTvlMap.put(key[0], tokenTvlB);
                                   }
                               }else {
                                   logger.warn("token long name is null: {}", pair);
                               }
                           }
                       }
                   }else {
                       logger.warn("get price is null: {}, {}", key, timestamp);
                   }
                   if(poolStat.getTvlA() == null && poolStat.getTvlB() != null) {
                       poolStat.setTvlA(poolStat.getTvlB());
                   }
                   if(poolStat.getTvlB() == null && poolStat.getTvlA() != null) {
                       poolStat.setTvlB(poolStat.getTvlA());
                   }
                   //update total tvl
                   if(swapStat.getTvl() == null) {
                       swapStat.setTvl(poolStat.getTvlA().add(poolStat.getTvlB()));
                   }else {
                       swapStat.setTvl(swapStat.getTvl().add(poolStat.getTvlA().add(poolStat.getTvlB())));
                   }
                   logger.info("update swapPoolStat: {}", poolStat);
                   swapPoolStatService.updateTvlAndAmount(poolStat);
                   logger.info("update swapPoolStat ok");
               }
               //update token tvl
               if(!tokenTvlMap.isEmpty()) {
                    for(String key: tokenTvlMap.keySet()) {
                        TokenTvl tokenTvl = tokenTvlMap.get(key);
                        TokenStat tokenStat = new TokenStat(key, wholeDateTime);
                        tokenStat.setTvl(tokenTvl.getTvl());
                        tokenStat.setTvlAmount(tokenTvl.getTvlAmount());
                        logger.info("update token stat: {}", tokenStat);
                        tokenStatService.updateTvl(tokenStat);
                        logger.info("update token stat ok");
                    }
               }
               //update total tvl
               swapStatService.updateTvl(swapStat.getStatDate(), swapStat.getTvl());
               logger.info("update total tvl: {}", swapStat);
           }
        } catch (JSONRPC2SessionException | IOException e) {
           logger.error("handle tvl update err:", e);
        }
    }
}
