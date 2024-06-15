package org.starcoin.indexer.handler;

import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.starcoin.api.ContractRPCClient;
import org.starcoin.api.StateRPCClient;
import org.starcoin.api.TokenContractRPCClient;
import org.starcoin.bean.*;
import org.starcoin.constant.Constant;
import org.starcoin.jsonrpc.client.JSONRPC2SessionException;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static org.starcoin.constant.Constant.STAR_TOKEN_OR_TAG;
import static org.starcoin.constant.Constant.STC_TOKEN_OR_TAG;
import static org.starcoin.indexer.handler.ServiceUtils.getTokenInfo;

@Service
public class MarketCapHandle {
    private static final Logger logger = LoggerFactory.getLogger(MarketCapHandle.class);
    private final RestHighLevelClient client;
    @Value("${starcoin.network}")
    private String network;

    @Autowired
    private TokenContractRPCClient tokenContractRPCClient;
    @Autowired
    private ContractRPCClient contractRPCClient;
    @Autowired
    private StateRPCClient stateRPCClient;

    public MarketCapHandle(RestHighLevelClient client) {
        this.client = client;
    }

    @PostConstruct
    public void initIndexes() {
        logger.info("init market cap indices...");
        try {
            ServiceUtils.createIndexIfNotExist(client, network, Constant.MARKET_CAP_INDEX);
            logger.info(" market cap index init ok!");
        } catch (IOException e) {
            logger.error("init market cap index error:", e);
        }
    }

    public void bulk(List<TokenMarketCap> tokenMarketCapList) {
        if (tokenMarketCapList != null && !tokenMarketCapList.isEmpty()) {
            //update from chain
            updateMarketCap(tokenMarketCapList);
            BulkRequest bulkRequest = new BulkRequest();
            for (TokenMarketCap marketCap : tokenMarketCapList) {
                logger.info("market cap: {}", marketCap);
                //factor
                TokenInfo tokenInfo = getTokenInfo(stateRPCClient, marketCap.getTypeTag());
                if (tokenInfo != null) {
                    BigDecimal value = new BigDecimal(marketCap.getMarketCap()).movePointLeft((int) Math.log10(tokenInfo.getScalingFactor()));
                    marketCap.setMarketCap(value.toBigInteger());
                } else {
                    logger.warn("when handle market cap, token info not exist: {}", marketCap.getTypeTag());
                }
                bulkRequest.add(buildMarketCapRequest(marketCap));
            }
            try {
                BulkResponse response = client.bulk(bulkRequest, RequestOptions.DEFAULT);
                logger.info("bulk market cap result: {}", response.buildFailureMessage());
            } catch (IOException e) {
                logger.error("bulk block error:", e);
            }
        }
    }

    private void updateMarketCap(List<TokenMarketCap> tokenMarketCapList) {
        if(tokenMarketCapList.isEmpty()) return;
        for (TokenMarketCap marketCap : tokenMarketCapList) {
            try {
                if (marketCap.getTypeTag().equals(STC_TOKEN_OR_TAG)) {
                    marketCap.setMarketCap(tokenContractRPCClient.getSTCCurrentSupply());
                }else if(marketCap.getTypeTag().equals(STAR_TOKEN_OR_TAG)) {
                    marketCap.setMarketCap(getStarMarketCap());
                }
                else {
                    marketCap.setMarketCap(tokenContractRPCClient.getTokenCurrentSupply(marketCap.getTypeTag()));
                }
            } catch (Exception e) {
                logger.error("get market cap err:", e);
            }
        }
    }

    private UpdateRequest buildMarketCapRequest(TokenMarketCap marketCap) {
        String marketCapIndex = ServiceUtils.getIndex(network, Constant.MARKET_CAP_INDEX);

        try {
            XContentBuilder builder = XContentFactory.jsonBuilder();
            String typeTag = marketCap.getTypeTag();
            builder.startObject();
            {
                builder.field("type_tag", marketCap.getTypeTag());
                builder.field("market_cap", String.valueOf(marketCap.getMarketCap()));
            }
            builder.endObject();
            IndexRequest indexRequest = new IndexRequest(marketCapIndex);
            indexRequest.id(typeTag).source(builder);
            UpdateRequest updateRequest = new UpdateRequest();
            updateRequest.index(marketCapIndex);
            updateRequest.id(typeTag);
            updateRequest.doc(builder);
            updateRequest.upsert(indexRequest);
            return updateRequest;
        } catch (IOException e) {
            logger.error("build market cap error:", e);
        }
        return null;
    }

    public BigInteger getStarMarketCap() {
        try {
            ContractCall call = new ContractCall();
            call.setFunctionId("0x8c109349c6bd91411d6bc962e080c4a3::TokenSwapGov::get_circulating_supply");

            call.setTypeArgs(new ArrayList<>());
            call.setArgs(new ArrayList<>());

            call.setArgs(new ArrayList<>());

            List result = contractRPCClient.call(call);
            if (result.size() > 0) {
                long cap = (Long) result.get(0);
                return BigInteger.valueOf(cap);
            }
        } catch (JSONRPC2SessionException e) {
            logger.warn("call contract function star market cap failure:", e);
        }
        return null;
    }

}
