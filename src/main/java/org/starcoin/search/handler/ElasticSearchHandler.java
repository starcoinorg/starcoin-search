package org.starcoin.search.handler;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.novi.serde.Bytes;
import com.novi.serde.DeserializationError;
import com.thetransactioncompany.jsonrpc2.client.JSONRPC2SessionException;
import org.bouncycastle.util.Arrays;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.GetMappingsRequest;
import org.elasticsearch.client.indices.GetMappingsResponse;
import org.elasticsearch.client.indices.PutMappingRequest;
import org.elasticsearch.cluster.metadata.MappingMetadata;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.script.Script;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.starcoin.api.Result;
import org.starcoin.api.StateRPCClient;
import org.starcoin.api.TransactionRPCClient;
import org.starcoin.bean.*;
import org.starcoin.search.bean.BlockOffset;
import org.starcoin.search.bean.SwapTransaction;
import org.starcoin.search.bean.SwapType;
import org.starcoin.search.bean.TransactionPayloadInfo;
import org.starcoin.search.constant.Constant;
import org.starcoin.search.service.SwapTxnService;
import org.starcoin.search.service.TransactionPayloadService;
import org.starcoin.search.utils.StructTagUtil;
import org.starcoin.search.utils.SwapApiClient;
import org.starcoin.types.AccountAddress;
import org.starcoin.types.StructTag;
import org.starcoin.types.TokenCode;
import org.starcoin.types.TransactionPayload;
import org.starcoin.types.event.DepositEvent;
import org.starcoin.types.event.WithdrawEvent;
import org.starcoin.utils.Hex;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

import static org.starcoin.api.TokenContractRPCClient.STCTypeTag;
import static org.starcoin.search.constant.Constant.ELASTICSEARCH_MAX_HITS;
import static org.starcoin.search.handler.ServiceUtils.tokenCache;

@Service
public class ElasticSearchHandler {

    private static final Logger logger = LoggerFactory.getLogger(ElasticSearchHandler.class);
    private final RestHighLevelClient client;
    private final StateRPCClient stateRPCClient;
    private final TransactionRPCClient transactionRPCClient;
    @Autowired
    private TransactionPayloadService transactionPayloadService;

    @Value("${starcoin.network}")
    private String network;
    private Set<TokenCode> tokenCodeList = new HashSet<>();

    private String blockIdsIndex;
    private String blockContentIndex;
    private String transactionIndex;
    private String uncleBlockIndex;
    private String eventIndex;
    private String pendingTxnIndex;
    private String transferIndex;
    private String payloadIndex;
    private String addressHolderIndex;
    private String transferJournalIndex;
    private String tokenInfoIndex;
    private XContentBuilder deletedBuilder;

    public ElasticSearchHandler(RestHighLevelClient client, StateRPCClient stateRPCClient, TransactionRPCClient transactionRPCClient) {
        this.client = client;
        this.stateRPCClient = stateRPCClient;
        this.transactionRPCClient = transactionRPCClient;
    }

    public RestHighLevelClient getClient() {
        return client;
    }

    @PostConstruct
    public void initIndexes() {
        logger.info("init indices...");
        try {
            blockIdsIndex = ServiceUtils.createIndexIfNotExist(client, network, Constant.BLOCK_IDS_INDEX);
            blockContentIndex = ServiceUtils.createIndexIfNotExist(client, network, Constant.BLOCK_CONTENT_INDEX);
            uncleBlockIndex = ServiceUtils.createIndexIfNotExist(client, network, Constant.UNCLE_BLOCK_INDEX);
            transactionIndex = ServiceUtils.createIndexIfNotExist(client, network, Constant.TRANSACTION_INDEX);
            eventIndex = ServiceUtils.createIndexIfNotExist(client, network, Constant.EVENT_INDEX);
            pendingTxnIndex = ServiceUtils.createIndexIfNotExist(client, network, Constant.PENDING_TXN_INDEX);
            transferIndex = ServiceUtils.createIndexIfNotExist(client, network, Constant.TRANSFER_INDEX);
            payloadIndex = ServiceUtils.createIndexIfNotExist(client, network, Constant.PAYLOAD_INDEX);
            addressHolderIndex = ServiceUtils.createIndexIfNotExist(client, network, Constant.ADDRESS_INDEX);
            transferJournalIndex = ServiceUtils.createIndexIfNotExist(client, network, Constant.TRANSFER_JOURNAL_INDEX);
            tokenInfoIndex = ServiceUtils.createIndexIfNotExist(client, network, Constant.TOKEN_INFO_INDEX);
            deletedBuilder = deletedBuilder();
            logger.info("index init ok!");
            loadTokenInfo();
        } catch (IOException e) {
            logger.error("init index error:", e);
        }
    }

    public BlockOffset getRemoteOffset() {
        GetMappingsRequest request = new GetMappingsRequest();
        try {
            request.indices(blockContentIndex);
            GetMappingsResponse response = client.indices().getMapping(request, RequestOptions.DEFAULT);
            MappingMetadata data = response.mappings().get(blockContentIndex);
            Object meta = data.getSourceAsMap().get("_meta");
            if (meta != null) {
                Map<String, Object> tip = (Map<String, Object>) ((LinkedHashMap<String, Object>) meta).get("tip");
                String blockHash = tip.get("block_hash").toString();
                Integer blockHeight = (Integer) tip.get("block_number");
                return new BlockOffset(blockHeight.longValue(), blockHash);
            }
        } catch (Exception e) {
            logger.error("get remote offset error:", e);
        }
        return null;
    }

    public void setRemoteOffset(BlockOffset blockOffset) {
        PutMappingRequest request = new PutMappingRequest(blockContentIndex);
        try {
            XContentBuilder builder = XContentFactory.jsonBuilder();
            builder.startObject();
            {
                builder.startObject("_meta");
                {
                    builder.startObject("tip");
                    {
                        builder.field("block_hash", blockOffset.getBlockHash());
                        builder.field("block_number", blockOffset.getBlockHeight());
                    }
                    builder.endObject();
                }
                builder.endObject();
            }
            builder.endObject();
            request.source(builder);
            client.indices().putMapping(request, RequestOptions.DEFAULT);
            logger.info("remote offset update ok : {}", blockOffset);
        } catch (Exception e) {
            logger.error("get remote offset error:", e);
        }
    }

    public Block getBlockId(long blockNumber) {
        SearchRequest searchRequest = new SearchRequest(blockIdsIndex);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        TermQueryBuilder termQueryBuilder = QueryBuilders.termQuery("header.number", blockNumber);
        searchSourceBuilder.query(termQueryBuilder);
        searchSourceBuilder.timeout(TimeValue.timeValueSeconds(5));
        searchRequest.source(searchSourceBuilder);
        SearchResponse searchResponse;
        try {
            searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            logger.error("get block by height error:", e);
            return null;
        }
        Result<Block> result = ServiceUtils.getSearchResult(searchResponse, Block.class);
        List<Block> blocks = result.getContents();
        if (blocks.size() == 1) {
            return blocks.get(0);
        } else {
            logger.warn("get block by height is null, network: {}, : {}", network, blockNumber);
        }
        return null;
    }

    public Block getBlockContent(String blockHash) {
        GetRequest getRequest = new GetRequest(blockContentIndex, blockHash);
        GetResponse getResponse = null;
        try {
            getResponse = client.get(getRequest, RequestOptions.DEFAULT);
            if (getResponse.isExists()) {
                String sourceAsString = getResponse.getSourceAsString();
                return JSON.parseObject(sourceAsString, Block.class);
            } else {
                logger.error("not found block by id: {}", blockHash);
            }
        } catch (IOException e) {
            logger.error("get block content: ", e);
        }
        return null;
    }


    public Result<Block> getBlockIds(long blockNumber, int count) {
        SearchRequest searchRequest = new SearchRequest(blockIdsIndex);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        RangeQueryBuilder termQueryBuilder = QueryBuilders.rangeQuery("header.number").gt(blockNumber);
        searchSourceBuilder.query(termQueryBuilder);
        searchSourceBuilder.timeout(TimeValue.timeValueSeconds(5));
        searchSourceBuilder.size(count);
        searchSourceBuilder.sort("header.number");
        searchRequest.source(searchSourceBuilder);
        SearchResponse searchResponse;
        try {
            searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            logger.error("get block by height error:", e);
            return null;
        }
        return ServiceUtils.getSearchResult(searchResponse, Block.class);
    }

    public void updateBlock(List<Block> blocks) {
        for (Block block : blocks
        ) {
            String id = String.valueOf(block.getHeader().getHeight());
            UpdateRequest updateRequest = new UpdateRequest();
            updateRequest.index(blockIdsIndex);
            updateRequest.id(id);
            IndexRequest indexRequest = new IndexRequest(blockIdsIndex);
            XContentBuilder blockBuild = getBlockBuilder(block);
            indexRequest.id(id).source(blockBuild);
            updateRequest.doc(blockBuild);
            updateRequest.upsert(indexRequest);
            try {
                client.update(updateRequest, RequestOptions.DEFAULT);
                logger.info("update block ids ok, {}", block.getHeader().getHeight());
            } catch (IOException e) {
                logger.error("update block id error:", e);
                return;
            }
        }
        bulk(blocks);
    }

    public void bulk(List<Block> blockList) {
        if (blockList.isEmpty()) {
            logger.warn("block list is empty");
            return;
        }
        BulkRequest bulkRequest = new BulkRequest();
        long minTimestamp = Long.MAX_VALUE;

        for (Block block : blockList) {
            //transform difficulty
            BlockHeader header = block.getHeader();
            transferDifficulty(header);
            block.setHeader(header);
            //add block ids
            bulkRequest.add(buildBlockRequest(block, blockIdsIndex));
            //  add block content
            IndexRequest blockContent = new IndexRequest(blockContentIndex);
            blockContent.id(block.getHeader().getBlockHash()).source(JSON.toJSONString(block), XContentType.JSON);
            bulkRequest.add(blockContent);
            Set<AddressHolder> holderAddress = new HashSet<>();

            //add transactions
            for (Transaction transaction : block.getTransactionList()) {
                IndexRequest transactionReq = new IndexRequest(transactionIndex);
                transactionReq.id(transaction.getTransactionHash()).source(JSON.toJSONString(transaction), XContentType.JSON);
                bulkRequest.add(transactionReq);
                //delete pending txn
                DeleteRequest deleteRequest = new DeleteRequest(pendingTxnIndex);
                deleteRequest.id(transaction.getTransactionHash());
                bulkRequest.add(deleteRequest);
                //add events
                List<Event> events = transaction.getEvents();
                if (events != null && events.size() > 0) {
                    for (Event event : events) {
                        bulkRequest.add(buildEventRequest(event, header.getAuthor(), transaction.getTimestamp(), eventIndex, holderAddress));
                    }
                }
                //add transfer
                List<IndexRequest> transferRequests = buildTransferRequest(transaction, transferIndex);
                if (!transferRequests.isEmpty()) {
                    for (IndexRequest request : transferRequests) {
                        bulkRequest.add(request);
                    }
                }
                if (minTimestamp > transaction.getTimestamp()) {
                    minTimestamp = transaction.getTimestamp();
                }
            }
            //add holder
            if (!holderAddress.isEmpty()) {
                for (AddressHolder holder : holderAddress
                ) {
                    updateAddressHolder(bulkRequest, holder);
                }
            }
            //add uncles
            for (BlockHeader uncle : block.getUncles()) {
                bulkRequest.add(buildUncleRequest(uncle, header.getHeight(), uncleBlockIndex));
            }
        }
        try {
            BulkResponse response = client.bulk(bulkRequest, RequestOptions.DEFAULT);
            logger.info("bulk block result: {}", response.buildFailureMessage());
        } catch (IOException e) {
            logger.error("bulk block error:", e);
        }
        //flush token list
        if (!tokenCodeList.isEmpty()) {
            for (TokenCode token : tokenCodeList) {
                try {
                    String codeStr = token.address + "::" + token.module + "::" + token.name;
                    TokenInfo tokenInfo = stateRPCClient.getTokenInfo(token.address.toString(), codeStr);
                    addTokenInfo(tokenInfo, codeStr);
                    //add to cache
                    tokenCache.put(codeStr, tokenInfo);
                } catch (JSONRPC2SessionException e) {
                    logger.error("flush token error:", e);
                }
            }
            tokenCodeList.clear();
        }
    }


    private void updateAddressHolder(BulkRequest bulkRequest, AddressHolder holder) {
        long amount = stateRPCClient.getAddressAmount(holder.address, holder.getTokenCode());
        if (amount == -1) {
            //resource not exist
            DeleteRequest deleteRequest = new DeleteRequest(addressHolderIndex);
            deleteRequest.id(holder.address + "-" + holder.tokenCode);
            bulkRequest.add(deleteRequest);
        } else {
            bulkRequest.add(buildHolderRequest(holder, amount));
        }
    }

    public void loadTokenInfo() {
        SearchRequest searchRequest = new SearchRequest(tokenInfoIndex);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.matchAllQuery());
        searchRequest.source(searchSourceBuilder);
        SearchResponse searchResponse;
        try {
            searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
            Result<TokenInfo> result = ServiceUtils.getSearchResult(searchResponse, TokenInfo.class);
            List<TokenInfo> tokenInfoList = result.getContents();
            if (!tokenInfoList.isEmpty()) {
                for (TokenInfo tokenInfo : tokenInfoList) {
                    tokenCache.put(tokenInfo.getTokenCode(), tokenInfo);
                }
                logger.info("load token info to cache ok: {}", tokenInfoList.size());
            }
        } catch (IOException e) {
            logger.error("get token infos error:", e);
        }
    }

    public void addTokenInfo(TokenInfo tokenInfo, String tokenCode) {
        IndexRequest request = new IndexRequest(tokenInfoIndex);
        XContentBuilder builder = null;
        try {
            builder = XContentFactory.jsonBuilder();
            builder.startObject();
            builder.field("token_code", tokenInfo.getTokenCode());
            builder.field("total_value", String.valueOf(tokenInfo.getTotalValue()));
            builder.field("scaling_factor", tokenInfo.getScalingFactor());
            builder.endObject();
        } catch (IOException e) {
            logger.error("build token_info error:", e);
        }
        request.id(tokenCode);
        request.source(builder);
        try {
            IndexResponse response = client.index(request, RequestOptions.DEFAULT);
            logger.info("add token info ok: {}", response.getResult());
        } catch (IOException e) {
            logger.error("add token info error:", e);
        }
    }

    public void updateMapping() {
        //read one index
        SearchRequest searchRequest = new SearchRequest(blockContentIndex);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.matchAllQuery()).size(1);

        searchRequest.source(searchSourceBuilder);
        SearchResponse searchResponse;
        try {
            searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
            SearchHit[] hits = searchResponse.getHits().getHits();
            if (hits.length > 0) {
                UpdateRequest updateRequest = new UpdateRequest();
                updateRequest.index(blockContentIndex);
                updateRequest.id(hits[0].getId());
                updateRequest.script(Script.parse("if (ctx._source.deleted == null) {ctx._source.deleted = false}"));
                UpdateResponse response = client.update(updateRequest, RequestOptions.DEFAULT);
                logger.info("update result: {}", response.toString());
            }
        } catch (IOException e) {
            logger.error("update error:", e);
        }
    }

    public void insertToken(String tokenCode) {
        TokenInfo tokenInfo = null;
        try {
            tokenInfo = stateRPCClient.getTokenInfo(tokenCode.substring(0, 34), tokenCode);
            if (tokenInfo != null) {
                addTokenInfo(tokenInfo, tokenCode);
            } else {
                logger.info("token info is null:{}", tokenCode);
            }
        } catch (Exception e) {
            logger.error("insert token error:", e);
        }
        logger.info("insert token ok: {}", tokenCode);
    }

    public void bulkForkedUpdate(Block block) {
        if (block == null) return;
        String blockHash = block.getHeader().getBlockHash();
        String blockAuthor = block.getHeader().getAuthor();
        BulkRequest bulkRequest = new BulkRequest();
        //delete ids
        DeleteRequest deleteRequest = new DeleteRequest(blockIdsIndex);
        deleteRequest.id(String.valueOf(block.getHeader().getHeight()));
        bulkRequest.add(deleteRequest);

        //delete block content
        addUpdateRequest(blockContentIndex, blockHash, bulkRequest);
        //delete transaction
        List<Transaction> transactionList = block.getTransactionList();
        Set<AddressHolder> holderAddress = new HashSet<>();
        if (transactionList != null && !transactionList.isEmpty()) {
            String transactionHash = "";
            List<String> transactionHashes = new ArrayList<>();
            for (Transaction transaction : transactionList) {
                transactionHash = transaction.getTransactionHash();
                transactionHashes.add(transactionHash);
                addUpdateRequest(transactionIndex, transactionHash, bulkRequest);
                //delete payload
                addUpdateRequest(payloadIndex, transactionHash, bulkRequest);
            }
            //delete event
            if (!transactionHashes.isEmpty()) {
                List<EventFull> events = getEventsByTransaction(transactionHashes);
                if (events != null && !events.isEmpty()) {
                    for (EventFull event : events) {
                        addUpdateRequest(eventIndex, event.getId(), bulkRequest);
                        Struct struct = Struct.fromRPC(event.getTypeTag());
                        if (struct != null) {
                            addToHolders(event, struct.getName(), struct.getModule(), blockAuthor, holderAddress, event.getEventAddress());
                        }
                    }
                }
                //delete transfer
                List<Transfer> transferList = getTransferByHash(network, transactionHashes);
                if (!transferList.isEmpty()) {
                    List<String> transferIdList = new ArrayList<>();
                    for (Transfer transfer : transferList
                    ) {
                        addUpdateRequest(transferIndex, transfer.getId(), bulkRequest);
                        transferIdList.add(transfer.getId());
                    }
                    //delete transfer_journal
                    //TODO history data must clear
                    List<TransferJournal> transferJournalList = getTransferJournals(network, transferIdList);
                    if (transferJournalList != null && !transferJournalList.isEmpty()) {
                        for (TransferJournal journal : transferJournalList) {
                            addUpdateRequest(transferJournalIndex, journal.getId(), bulkRequest);
                        }
                    }
                }
            }
        }
        //flush address holder
        if (!holderAddress.isEmpty()) {
            for (AddressHolder holder : holderAddress) {
                updateAddressHolder(bulkRequest, holder);
            }
        }

        //delete uncle
        List<BlockHeader> uncleHeaders = block.getUncles();
        if (!uncleHeaders.isEmpty()) {
            List<String> blockHashes = new ArrayList<>();
            for (BlockHeader header : uncleHeaders) {
                blockHashes.add(header.getBlockHash());
            }
            List<UncleBlock> uncles = getUncleBlockByHash(network, block.getHeader().getHeight(), blockHashes);
            if (uncles != null && !uncles.isEmpty()) {
                for (UncleBlock uncle : uncles) {
                    addUpdateRequest(uncleBlockIndex, uncle.getId(), bulkRequest);
                }
            }
        }
        try {
            BulkResponse response = client.bulk(bulkRequest, RequestOptions.DEFAULT);
            logger.info("bulk forked result: {}", response.buildFailureMessage());
        } catch (IOException e) {
            logger.error("bulk forked error:", e);
        }
    }

    private List<TransferJournal> getTransferJournals(String network, List<String> transferIds) {
        SearchRequest searchRequest = new SearchRequest(transferJournalIndex);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.termsQuery("transfer_id", transferIds));
        searchRequest.source(searchSourceBuilder);
        SearchResponse searchResponse;
        try {
            searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            logger.error("get uncle block by hash error:", e);
            return null;
        }
        Result<TransferJournal> result = ServiceUtils.getSearchResult(searchResponse, TransferJournal.class);
        return result.getContents();
    }

    private List<UncleBlock> getUncleBlockByHash(String network, long blockHeight, List<String> blockHashes) {
        SearchRequest searchRequest = new SearchRequest(uncleBlockIndex);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
        boolQueryBuilder.must(QueryBuilders.termQuery("uncle_block_number", blockHeight));
        boolQueryBuilder.must(QueryBuilders.termsQuery("header.block_hash", blockHashes));
        searchSourceBuilder.query(boolQueryBuilder);
        searchRequest.source(searchSourceBuilder);
        SearchResponse searchResponse;
        try {
            searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            logger.error("get uncle block by hash error:", e);
            return null;
        }
        Result<UncleBlock> result = ServiceUtils.getSearchResult(searchResponse, UncleBlock.class);
        return result.getContents();
    }

    public List<EventFull> getEventsByTransaction(List<String> txnHashes) {
        SearchRequest searchRequest = new SearchRequest(eventIndex);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.size(ELASTICSEARCH_MAX_HITS);
        //begin offset
        int offset = 0;
        searchSourceBuilder.from(0);

        BoolQueryBuilder exersiceBoolQuery = QueryBuilders.boolQuery();
        exersiceBoolQuery.should(QueryBuilders.termsQuery("transaction_hash", txnHashes));
        searchSourceBuilder.query(exersiceBoolQuery).fetchSource(new String[]{"_id", "event_address", "data"}, null);
        searchRequest.source(searchSourceBuilder);
        searchSourceBuilder.trackTotalHits(true);
        searchSourceBuilder.sort("timestamp", SortOrder.DESC);

        SearchResponse searchResponse = null;
        try {
            searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            logger.error("get events:", e);
        }
        Result<EventFull> result = ServiceUtils.getSearchResult(searchResponse, EventFull.class);
        return result.getContents();
    }

    private void addUpdateRequest(String indexName, String id, BulkRequest bulkRequest) {
        UpdateRequest updateRequest = new UpdateRequest();
        updateRequest.index(indexName);
        updateRequest.id(id);
        updateRequest.doc(deletedBuilder);
        bulkRequest.add(updateRequest);
    }

    public void bulkAddPayload(String payloadIndex, List<Transaction> transactionList, ObjectMapper objectMapper, List<SwapTransaction> swapTransactionList) throws IOException, DeserializationError {
        BulkRequest bulkRequest = new BulkRequest();
        List<org.starcoin.search.bean.TransactionPayload> transactionPayloadList = new ArrayList<>();
        for (Transaction transaction : transactionList) {
            if (transaction.getUserTransaction() != null) {
                RawTransaction rawTransaction = transaction.getUserTransaction().getRawTransaction();
                String payload = rawTransaction.getPayload();
                TransactionPayload transactionPayload = TransactionPayload.bcsDeserialize(Hex.decode(payload));
                TransactionPayloadInfo payloadInfo = new TransactionPayloadInfo(transactionPayload, transaction.getTimestamp(), transaction.getTransactionHash());
                //swap txn filter
                if (transactionPayload instanceof TransactionPayload.ScriptFunction) {
                    TransactionPayload.ScriptFunction scriptFunctionPayload = ((TransactionPayload.ScriptFunction) transactionPayload);
                    String function = scriptFunctionPayload.value.function.toString();
                    if (SwapType.isSwapType(function)) {
                        //todo add farm swap function
                        if (scriptFunctionPayload.value.ty_args.size() > 1 && scriptFunctionPayload.value.args.size() > 1) {
                            //parse token and amount
                            String tokenA = StructTagUtil.structTagToString(((org.starcoin.types.TypeTag.Struct) scriptFunctionPayload.value.ty_args.get(0)).value);
                            String tokenB = StructTagUtil.structTagToString(((org.starcoin.types.TypeTag.Struct) scriptFunctionPayload.value.ty_args.get(1)).value);
                            BigInteger argFirst = ServiceUtils.deserializeU128(scriptFunctionPayload.value.args.get(0));
                            BigInteger argSecond = ServiceUtils.deserializeU128(scriptFunctionPayload.value.args.get(1));
                            SwapTransaction swapTransaction = new SwapTransaction();
                            swapTransaction.setTransactionHash(transaction.getTransactionHash());
                            swapTransaction.setTimestamp(transaction.getTimestamp());
                            swapTransaction.setAccount(rawTransaction.getSender());
                            swapTransaction.setTokenA(tokenA);
                            swapTransaction.setTokenB(tokenB);
                            swapTransaction.setAmountA(new BigDecimal(argFirst));
                            swapTransaction.setAmountB(new BigDecimal(argSecond));
                            swapTransaction.setSwapType(SwapType.fromValue(function));
                            swapTransactionList.add(swapTransaction);
                        } else {
                            logger.warn("swap txn args error: {}", transaction.getTransactionHash());
                        }
                    }
                }
                IndexRequest request = new IndexRequest(payloadIndex);
                request.id(transaction.getTransactionHash()).source(objectMapper.writeValueAsString(payloadInfo), XContentType.JSON);
                bulkRequest.add(request);
                org.starcoin.search.bean.TransactionPayload payload1 = new org.starcoin.search.bean.TransactionPayload();
                payload1.setTransactionHash(transaction.getTransactionHash());
                payload1.setJson(ServiceUtils.getJsonString(transactionPayload));
                transactionPayloadList.add(payload1);
            }
        }
        BulkResponse response = client.bulk(bulkRequest, RequestOptions.DEFAULT);
        logger.info("bulk txn payload result: {}", response.buildFailureMessage());

        if (!transactionPayloadList.isEmpty()) {
            transactionPayloadService.savePayload(transactionPayloadList);
            logger.info("save txn payload to pg ok: {}", transactionPayloadList.size());
        }
    }

    private void transferDifficulty(BlockHeader header) {
        String difficultyStr = header.getDifficultyHexStr();
        if (difficultyStr.startsWith("0x")) {
            try {
                long difficulty = Long.parseLong(difficultyStr.substring(2), 16);
                if (difficulty > 0) {
                    header.setDifficulty(difficulty);
                }
            } catch (NumberFormatException e) {
                logger.error("transform difficulty error: {}", difficultyStr);
            }
        }
    }

    private IndexRequest buildBlockRequest(Block bLock, String indexName) {
        IndexRequest request = new IndexRequest(indexName);
        XContentBuilder builder = getBlockBuilder(bLock);
        request.id(String.valueOf(bLock.getHeader().getHeight())).source(builder);
        return request;
    }

    private XContentBuilder getBlockBuilder(Block bLock) {
        XContentBuilder builder = null;
        BlockHeader header = bLock.getHeader();
        BlockMetadata metadata = bLock.getBlockMetadata();
        try {
            builder = XContentFactory.jsonBuilder();
            builder.startObject();
            {
                builder.field("timestamp", header.getTimestamp());
                builder.startObject("header");
                {
                    blockHeaderBuilder(builder, header);
                }
                builder.endObject();
                if (metadata != null) {
                    builder.startObject("metadata");
                    {
                        builder.field("author", metadata.getAuthor());
                        builder.field("parentHash", metadata.getParentHash());
                        builder.field("timestamp", metadata.getTimestamp());
                        builder.field("author_auth_key", metadata.getAuthorAuthKey());
                        builder.field("uncles", metadata.getUncles());
                        builder.field("number", metadata.getNumber());
                        builder.field("chain_id", header.getChainId());
                        builder.field("parent_gas_used", metadata.getParentGasUsed());
                    }
                    builder.endObject();
                }
            }
            builder.endObject();
        } catch (IOException e) {
            logger.error("build block error:", e);
        }
        return builder;
    }

    private void blockHeaderBuilder(XContentBuilder builder, BlockHeader header) throws IOException {
        builder.field("author", header.getAuthor());
        builder.field("author_auth_key", header.getAuthorAuthKey());
        builder.field("block_accumulator_root", header.getBlockAccumulatorRoot());
        builder.field("block_hash", header.getBlockHash());
        builder.field("body_hash", header.getBodyHash());
        builder.field("chain_id", header.getChainId());
        builder.field("difficulty", header.getDifficultyHexStr());
        builder.field("difficulty_number", header.getDifficulty());
        builder.field("gas_used", header.getGasUsed());
        builder.field("number", header.getHeight());
        builder.field("parent_hash", header.getParentHash());
        builder.field("state_root", header.getStateRoot());
        builder.field("txn_accumulator_root", header.getTxnAccumulatorRoot());
        builder.field("timestamp", header.getTimestamp());
    }

    private IndexRequest buildUncleRequest(BlockHeader header, long blockHeaderHeight, String indexName) {
        //transfer difficulty
        transferDifficulty(header);
        IndexRequest request = new IndexRequest(indexName);
        XContentBuilder builder = null;
        try {
            builder = XContentFactory.jsonBuilder();
            builder.startObject();
            {
                builder.startObject("header");
                {
                    blockHeaderBuilder(builder, header);
                }
                builder.endObject();
                builder.field("uncle_block_number", blockHeaderHeight);
            }
            builder.endObject();
        } catch (IOException e) {
            logger.error("build block error:", e);
        }
        request.source(builder);
        return request;
    }

    private List<IndexRequest> buildTransferRequest(Transaction transaction, String indexName) {
        List<IndexRequest> requests = new ArrayList<>();
        UserTransaction userTransaction = transaction.getUserTransaction();
        if (userTransaction == null) {
            return requests;
        }
        RawTransaction rawTransaction = userTransaction.getRawTransaction();
        if (rawTransaction == null) {
            return requests;
        }
        org.starcoin.types.TransactionPayload payload = rawTransaction.getTransactionPayload();
        if (!(payload.getClass() == org.starcoin.types.TransactionPayload.ScriptFunction.class)) {
            //todo script and package handle
            logger.warn("other type must handle in future: {}", payload.getClass());
            return requests;
        }
        org.starcoin.types.ScriptFunction function = ((org.starcoin.types.TransactionPayload.ScriptFunction) payload).value;
        String functionName = function.function.value;
        if (functionName.equals("peer_to_peer") || functionName.equals("peer_to_peer_v2")) {
            Transfer transfer = new Transfer();
            transfer.setTxnHash(transaction.getTransactionHash());
            transfer.setSender(rawTransaction.getSender());
            transfer.setTimestamp(transaction.getTimestamp());
            transfer.setIdentifier(functionName);
            transfer.setReceiver(Hex.encode(function.args.get(0)));
            String amount = Hex.encode(function.args.get(1));
            if (functionName.equals("peer_to_peer")) {
                amount = Hex.encode(function.args.get(2));
            }
            transfer.setAmount(amount);
            transfer.setTypeTag(getTypeTags(function.ty_args));
            IndexRequest request = new IndexRequest(indexName);
            request.source(JSON.toJSONString(transfer), XContentType.JSON);
            requests.add(request);
            return requests;
        } else if (functionName.equals("batch_peer_to_peer") || functionName.equals("batch_peer_to_peer_v2")) {
            //batch handle
            Bytes addresses = function.args.get(0);
            byte[] addressBytes = addresses.content();
            byte[] amountBytes = function.args.get(2).content();
            if (functionName.equals("batch_peer_to_peer_v2")) {
                amountBytes = function.args.get(1).content();
            }
            int size = addressBytes.length / AccountAddress.LENGTH;
            for (int i = 0; i < size; i++) {
                byte[] addressByte = Arrays.copyOfRange(addressBytes, i * AccountAddress.LENGTH, (i + 1) * AccountAddress.LENGTH);
                AccountAddress address = AccountAddress.valueOf(addressByte);
                byte[] amountByte = Arrays.copyOfRange(amountBytes, i * AccountAddress.LENGTH, (i + 1) * AccountAddress.LENGTH);
                String amount = Hex.encode(amountByte);
                Transfer transfer = new Transfer();
                transfer.setTxnHash(transaction.getTransactionHash());
                transfer.setSender(rawTransaction.getSender());
                transfer.setTimestamp(transaction.getTimestamp());
                transfer.setIdentifier(functionName);
                transfer.setReceiver(address.toString());
                transfer.setAmount(amount);
                transfer.setTypeTag(getTypeTags(function.ty_args));
                IndexRequest request = new IndexRequest(indexName);
                request.source(JSON.toJSONString(transfer), XContentType.JSON);
                requests.add(request);
            }
            logger.info("batch transfer handle: {}", size);
        } else {
            logger.warn("other scripts not support: {}", function.function.value);
        }
        return requests;
    }

    private XContentBuilder deletedBuilder() {
        try {
            XContentBuilder builder = XContentFactory.jsonBuilder();
            builder.startObject();
            {
                builder.field("deleted", true);
            }
            builder.endObject();
            return builder;
        } catch (IOException e) {
            logger.error("deleted build error:", e);
            return null;
        }
    }

    private UpdateRequest buildHolderRequest(AddressHolder holder, long amount) {
        try {
            XContentBuilder builder = XContentFactory.jsonBuilder();
            String id = holder.address + "-" + holder.tokenCode;
            builder.startObject();
            {
                builder.field("address", holder.address);
                builder.field("type_tag", holder.tokenCode);
                builder.field("amount", amount);
            }
            builder.endObject();
            IndexRequest indexRequest = new IndexRequest(addressHolderIndex);
            indexRequest.id(id).source(builder);
            UpdateRequest updateRequest = new UpdateRequest();
            updateRequest.index(addressHolderIndex);
            updateRequest.id(id);
            updateRequest.doc(builder);
            updateRequest.upsert(indexRequest);
            return updateRequest;
        } catch (IOException e) {
            logger.error("build holder error:", e);
        }
        return null;
    }

    private String getTypeTags(List<org.starcoin.types.TypeTag> typeTags) {
        if (typeTags.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (org.starcoin.types.TypeTag typeTag : typeTags) {
            if (typeTag.getClass() == org.starcoin.types.TypeTag.Struct.class) {
                StructTag structTag = ((org.starcoin.types.TypeTag.Struct) typeTag).value;
                sb.append(structTag.address).append("::").append(structTag.module).append("::").append(structTag.name).append(";");
            }
        }
        if (sb.length() > 1) {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }

    private IndexRequest buildEventRequest(Event event, String blockAuthor, long timestamp, String indexName, Set<AddressHolder> holders) {
        IndexRequest request = new IndexRequest(indexName);
        XContentBuilder builder = null;
        try {
            Struct struct = Struct.fromRPC(event.getTypeTag());
            String eventAddress = event.eventCreateAddress();
            builder = XContentFactory.jsonBuilder();
            builder.startObject();
            builder.field("event_seq_number", event.getEventSeqNumber());
            builder.field("block_hash", event.getBlockHash());
            builder.field("block_number", event.getBlockNumber());
            builder.field("transaction_hash", event.getTransactionHash());
            builder.field("transaction_index", event.getTransactionIndex());
            builder.field("data", event.getData());
            builder.field("type_tag", event.getTypeTag());
            builder.field("event_key", event.getEventKey());
            builder.field("event_address", eventAddress);
            if (struct != null) {
                //add holder address
                String tagName = struct.getName();
                String tagAddress = struct.getAddress();
                String tagModule = struct.getModule();
                if (tagAddress.equals(Constant.EVENT_FILTER_ADDRESS)
                        && (tagName.equalsIgnoreCase(Constant.DEPOSIT_EVENT) ||
                        tagName.equalsIgnoreCase(Constant.WITHDRAW_EVENT))) {
                    addToHolders(event, tagName, tagModule, blockAuthor, holders, eventAddress);
                }
                builder.field("tag_address", tagAddress);
                builder.field("tag_module", tagModule);
                builder.field("tag_name", tagName);
            } else {
                logger.warn("type tag is not struct: {}", event.getTypeTag());
            }
            builder.field("timestamp", timestamp);
            builder.endObject();
        } catch (IOException e) {
            logger.error("build block error:", e);
        }
        request.source(builder);
        return request;
    }

    private void addToHolders(Event event, String tagName, String tagModule, String blockAuthor, Set<AddressHolder> holders, String eventAddress) {
        byte[] bytes = Hex.decode(event.getData());
        try {
            if (tagName.equalsIgnoreCase(Constant.DEPOSIT_EVENT)) {
                DepositEvent inner = DepositEvent.bcsDeserialize(bytes);
                String sb = inner.token_code.address +
                        "::" +
                        inner.token_code.module +
                        "::" +
                        inner.token_code.name;
                holders.add(new AddressHolder(eventAddress, sb));
                tokenCodeList.add(inner.token_code);
            } else if (tagName.equalsIgnoreCase(Constant.WITHDRAW_EVENT)) {
                if (tagModule.equalsIgnoreCase(Constant.EVENT_TREASURY_MODULE)) {
                    holders.add(new AddressHolder(blockAuthor, STCTypeTag));
                } else {
                    WithdrawEvent inner = WithdrawEvent.bcsDeserialize(bytes);
                    String sb = inner.token_code.address +
                            "::" +
                            inner.token_code.module +
                            "::" +
                            inner.token_code.name;
                    holders.add(new AddressHolder(eventAddress, sb));
                    tokenCodeList.add(inner.token_code);
                }
            }
        } catch (DeserializationError deserializationError) {
            logger.error("decode event data error:{}", event, deserializationError);
        }
    }

    public List<Transfer> getTransferByHash(String network, List<String> txnHashList) {
        List<Transfer> transfers = new ArrayList<>();
        SearchRequest searchRequest = new SearchRequest(transferIndex);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        //page size
        searchSourceBuilder.size(0);
        searchSourceBuilder.from(0);
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        boolQueryBuilder.must(QueryBuilders.termsQuery("txn_hash", txnHashList));
        searchSourceBuilder.query(boolQueryBuilder).fetchSource("_id", null);
        searchRequest.source(searchSourceBuilder);
        searchSourceBuilder.trackTotalHits(true);
        SearchResponse searchResponse;
        try {
            searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            logger.error("get transfer error:", e);
            return transfers;
        }
        Result<Transfer> result = ServiceUtils.getSearchResult(searchResponse, Transfer.class);
        transfers = result.getContents();
        return transfers;
    }

    public List<Transaction> getTransactionByTimestamp(String network, String timestamp) throws IOException {
        SearchRequest searchRequest = new SearchRequest(transactionIndex);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.size(20);

        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        RangeQueryBuilder termQueryBuilder = QueryBuilders.rangeQuery("timestamp").gt(timestamp);
        boolQuery.must(termQueryBuilder);
        boolQuery.must(QueryBuilders.rangeQuery("transaction_index").gt(0));
        searchSourceBuilder.query(boolQuery);

        searchRequest.source(searchSourceBuilder);
        searchSourceBuilder.sort("timestamp", SortOrder.ASC);

        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        Result<Transaction> result = ServiceUtils.getSearchResult(searchResponse, Transaction.class);

        List<Transaction> transactions = result.getContents();
        return transactions;
    }

    public void addUserTransactionToList(List<Transaction> transactionList) throws JSONRPC2SessionException {
        for (Transaction transaction : transactionList) {
            Transaction userTransaction = transactionRPCClient.getTransactionByHash(transaction.getTransactionHash());
            if (userTransaction != null) {
                transaction.setUserTransaction(userTransaction.getUserTransaction());
            } else {
                logger.warn("get transaction inner txn is null: {}", transaction.getTransactionHash());
            }
        }
    }

    static class AddressHolder {
        private final String address;
        private final String tokenCode;

        AddressHolder(String address, String tokenCode) {
            this.address = address;
            this.tokenCode = tokenCode;
        }

        public String getAddress() {
            return address;
        }

        public String getTokenCode() {
            return tokenCode;
        }

        @Override
        public String toString() {
            return "AddressHolder{" +
                    "address='" + address + '\'' +
                    ", tokenCode='" + tokenCode + '\'' +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            AddressHolder holder = (AddressHolder) o;
            return address.equals(holder.address) && tokenCode.equals(holder.tokenCode);
        }

        @Override
        public int hashCode() {
            return Objects.hash(address, tokenCode);
        }
    }

}
