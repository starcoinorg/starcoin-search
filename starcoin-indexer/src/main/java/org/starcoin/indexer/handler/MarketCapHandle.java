package org.starcoin.indexer.handler;

import com.alibaba.fastjson.JSON;
import com.thetransactioncompany.jsonrpc2.client.JSONRPC2SessionException;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.collapse.CollapseBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.starcoin.api.ContractRPCClient;
import org.starcoin.api.Result;
import org.starcoin.api.StateRPCClient;
import org.starcoin.api.TokenContractRPCClient;
import org.starcoin.bean.*;
import org.starcoin.constant.Constant;

import javax.annotation.PostConstruct;
import java.io.IOException;
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

    public Result<TokenMarketCap> getTokenMarketCap() {
        SearchRequest searchRequest = new SearchRequest(ServiceUtils.getIndex(network, Constant.ADDRESS_INDEX));
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        //page size
        searchSourceBuilder.size(Constant.ELASTICSEARCH_MAX_HITS);
        searchSourceBuilder.from(0);
        searchSourceBuilder.collapse(new CollapseBuilder("type_tag.keyword"));

        searchRequest.source(searchSourceBuilder);
        searchSourceBuilder.trackTotalHits(true);
        SearchResponse searchResponse;
        try {
            searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            logger.error("get token market cap error:", e);
            return Result.EmptyResult;
        }
        return getResult(searchResponse);
    }

    public void bulk(Result<TokenMarketCap> tokenMarketCapResult) {
        if (tokenMarketCapResult.getTotal() > 1) {
            List<TokenMarketCap> marketCaps = tokenMarketCapResult.getContents();
            BulkRequest bulkRequest = new BulkRequest();
            for (TokenMarketCap marketCap : marketCaps) {
                //factor
                TokenInfo tokenInfo = getTokenInfo(stateRPCClient, marketCap.getTypeTag());
                if (tokenInfo != null) {
                    marketCap.setMarketCap(marketCap.getMarketCap().divide(new BigInteger(String.valueOf(tokenInfo.getScalingFactor()))));
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

    private Result<TokenMarketCap> getResult(SearchResponse searchResponse) {
        SearchHit[] searchHit = searchResponse.getHits().getHits();
        Result<TokenMarketCap> result = new Result<>();
        result.setTotal(searchResponse.getHits().getTotalHits().value);
        List<TokenMarketCap> tokens = new ArrayList<>();
        for (SearchHit hit : searchHit) {
            TokenMarketCap marketCap = JSON.parseObject(hit.getSourceAsString(), TokenMarketCap.class);
            try {
                if (marketCap.getTypeTag().equals(STC_TOKEN_OR_TAG)) {
                    marketCap.setMarketCap(tokenContractRPCClient.getSTCCurrentSupply());
                }else if(marketCap.getTypeTag().equals(STAR_TOKEN_OR_TAG)) {
                    marketCap.setMarketCap(getStarMarketCap());
                }
                else {
                    marketCap.setMarketCap(tokenContractRPCClient.getTokenCurrentSupply(marketCap.getTypeTag()));
                }
                tokens.add(marketCap);
            } catch (JSONRPC2SessionException e) {
                logger.error("get market cap err:", e);
            }
        }
        result.setContents(tokens);
        return result;
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
