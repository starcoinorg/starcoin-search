package org.starcoin.search.handler;

import com.alibaba.fastjson.JSON;
import com.novi.serde.Bytes;
import com.novi.serde.DeserializationError;
import org.bouncycastle.util.Arrays;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.GetMappingsRequest;
import org.elasticsearch.client.indices.GetMappingsResponse;
import org.elasticsearch.client.indices.PutMappingRequest;
import org.elasticsearch.cluster.metadata.MappingMetadata;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.starcoin.api.StateRPCClient;
import org.starcoin.bean.*;
import org.starcoin.search.bean.Offset;
import org.starcoin.search.constant.Constant;
import org.starcoin.types.AccountAddress;
import org.starcoin.types.StructTag;
import org.starcoin.types.event.DepositEvent;
import org.starcoin.utils.Hex;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.*;

@Service
public class ElasticSearchHandler {

    private static final Logger logger = LoggerFactory.getLogger(ElasticSearchHandler.class);
    private final RestHighLevelClient client;
    private final StateRPCClient stateRPCClient;
    @Value("${starcoin.network}")
    private String network;

    public ElasticSearchHandler(RestHighLevelClient client, StateRPCClient stateRPCClient) {
        this.client = client;
        this.stateRPCClient = stateRPCClient;
    }

    @PostConstruct
    public void initIndexes() {
        logger.info("init indices...");
        try {
            ServiceUtils.createIndexIfNotExist(client, network, Constant.BLOCK_IDS_INDEX);
            ServiceUtils.createIndexIfNotExist(client, network, Constant.BLOCK_CONTENT_INDEX);
            ServiceUtils.createIndexIfNotExist(client, network, Constant.UNCLE_BLOCK_INDEX);
            ServiceUtils.createIndexIfNotExist(client, network, Constant.TRANSACTION_INDEX);
            ServiceUtils.createIndexIfNotExist(client, network, Constant.EVENT_INDEX);
            ServiceUtils.createIndexIfNotExist(client, network, Constant.PENDING_TXN_INDEX);
            ServiceUtils.createIndexIfNotExist(client, network, Constant.TRANSFER_INDEX);
            logger.info("index init ok!");
        } catch (IOException e) {
            logger.error("init index error:", e);
        }
    }

    public Offset getRemoteOffset() {
        GetMappingsRequest request = new GetMappingsRequest();
        try {
            String offsetIndex = ServiceUtils.getIndex(network, Constant.BLOCK_CONTENT_INDEX);
            request.indices(offsetIndex);
            GetMappingsResponse response = client.indices().getMapping(request, RequestOptions.DEFAULT);
            MappingMetadata data = response.mappings().get(offsetIndex);
            Object meta = data.getSourceAsMap().get("_meta");
            if (meta != null) {
                Map<String, Object> tip = (Map<String, Object>) ((LinkedHashMap<String, Object>) meta).get("tip");
                String blockHash = tip.get("block_hash").toString();
                Integer blockHeight = (Integer) tip.get("block_number");
                return new Offset(blockHeight.longValue(), blockHash);
            }
        } catch (Exception e) {
            logger.error("get remote offset error:", e);
        }
        return null;
    }

    public void setRemoteOffset(Offset offset) {
        String offsetIndex = ServiceUtils.getIndex(network, Constant.BLOCK_CONTENT_INDEX);
        PutMappingRequest request = new PutMappingRequest(offsetIndex);
        try {
            XContentBuilder builder = XContentFactory.jsonBuilder();
            builder.startObject();
            {
                builder.startObject("_meta");
                {
                    builder.startObject("tip");
                    {
                        builder.field("block_hash", offset.getBlockHash());
                        builder.field("block_number", offset.getBlockHeight());
                    }
                    builder.endObject();
                }
                builder.endObject();
            }
            builder.endObject();
            request.source(builder);
            client.indices().putMapping(request, RequestOptions.DEFAULT);
            logger.info("remote offset update ok : {}", offset);
        } catch (Exception e) {
            logger.error("get remote offset error:", e);
        }
    }


    public void bulk(List<Block> blockList, Set<Long> deleteForkBlockIds) {
        if (blockList.isEmpty()) {
            logger.warn("block list is empty");
            return;
        }
        BulkRequest bulkRequest = new BulkRequest();
        String blockIndex = ServiceUtils.getIndex(network, Constant.BLOCK_IDS_INDEX);
        String blockContentIndex = ServiceUtils.getIndex(network, Constant.BLOCK_CONTENT_INDEX);
        String uncleIndex = ServiceUtils.getIndex(network, Constant.UNCLE_BLOCK_INDEX);
        String txnIndex = ServiceUtils.getIndex(network, Constant.TRANSACTION_INDEX);
        String eventIndex = ServiceUtils.getIndex(network, Constant.EVENT_INDEX);
        String pendingIndex = ServiceUtils.getIndex(network, Constant.PENDING_TXN_INDEX);
        String transferIndex = ServiceUtils.getIndex(network, Constant.TRANSFER_INDEX);
        for (Block block : blockList) {
            //transform difficulty
            BlockHeader header = block.getHeader();
            transferDifficulty(header);
            block.setHeader(header);
            //add block ids
            if (deleteForkBlockIds.size() > 0) {
                for(long forkId: deleteForkBlockIds) {

                }
//                //fork block handle
//                if (header.getHeight() == deleteOrSkipIndex) {
//                    logger.warn("fork block, skip: {}", deleteOrSkipIndex);
//                } else {
//                    //上一轮已经添加ids，需要删掉
//                    DeleteRequest deleteRequest = new DeleteRequest(blockIndex);
//                    deleteRequest.id(String.valueOf(deleteOrSkipIndex));
//                    bulkRequest.add(deleteRequest);
//                }
            } else {
                bulkRequest.add(buildBlockRequest(block, blockIndex));
            }
            //  add block content
            IndexRequest blockContent = new IndexRequest(blockContentIndex);
            blockContent.id(block.getHeader().getBlockHash()).source(JSON.toJSONString(block), XContentType.JSON);
            bulkRequest.add(blockContent);
            //add transactions
            for (Transaction transaction : block.getTransactionList()) {
                IndexRequest transactionReq = new IndexRequest(txnIndex);
                transactionReq.id(transaction.getTransactionHash()).source(JSON.toJSONString(transaction), XContentType.JSON);
                bulkRequest.add(transactionReq);
                //delete pending txn
                DeleteRequest deleteRequest = new DeleteRequest(pendingIndex);
                deleteRequest.id(transaction.getTransactionHash());
                bulkRequest.add(deleteRequest);
                //add events
                List<Event> events = transaction.getEvents();
                if (events != null && events.size() > 0) {
                    Set<AddressHolder> holderAddress = new HashSet<>();
                    for (Event event : events) {
                        bulkRequest.add(buildEventRequest(event, transaction.getTimestamp(), eventIndex, holderAddress));
                    }
                    if (!holderAddress.isEmpty()) {
                        for (AddressHolder holder : holderAddress
                        ) {
                            long amount = stateRPCClient.getAddressAmount(holder.address);
                            bulkRequest.add(buildHolderRequest(holder, amount));
                        }
                    }
                }
                //add transfer
                List<IndexRequest> transferRequests = buildTransferRequest(transaction, transferIndex);
                if (!transferRequests.isEmpty()) {
                    for (IndexRequest request : transferRequests) {
                        bulkRequest.add(request);
                    }
                }
            }
            //add uncles
            for (BlockHeader uncle : block.getUncles()) {
                bulkRequest.add(buildUncleRequest(uncle, header.getHeight(), uncleIndex));
            }
        }
        try {
            BulkResponse response = client.bulk(bulkRequest, RequestOptions.DEFAULT);
            logger.info("bulk block result: {}", response.buildFailureMessage());
        } catch (IOException e) {
            logger.error("bulk block error:", e);
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
        request.id(String.valueOf(bLock.getHeader().getHeight())).source(builder);
        return request;
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

    private UpdateRequest buildHolderRequest(AddressHolder holder, long amount) {
        String addressIndex = ServiceUtils.getIndex(network, Constant.ADDRESS_INDEX);
        try {
            XContentBuilder builder = XContentFactory.jsonBuilder();
            builder.startObject();
            {
                builder.field("address", holder.address);
                builder.field("type_tag", holder.tokenCode);
                builder.field("amount", amount);
            }
            builder.endObject();
            IndexRequest indexRequest = new IndexRequest(addressIndex);
            indexRequest.id(holder.address).source(builder);
            UpdateRequest updateRequest = new UpdateRequest();
            updateRequest.index(addressIndex);
            updateRequest.id(holder.address);
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

    private IndexRequest buildEventRequest(Event event, long timestamp, String indexName, Set<AddressHolder> holders) {
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
                if (tagAddress.equals(Constant.EVENT_FILTER_ADDRESS) && tagModule.equals(Constant.EVENT_FILTER__MODULE)
                        && (tagName.equalsIgnoreCase(Constant.DEPOSIT_EVENT) || tagName.equalsIgnoreCase(Constant.WITHDRAW_EVENT))) {
                    try {
                        DepositEvent inner = DepositEvent.bcsDeserialize(Hex.decode(event.getData()));
                        String sb = inner.token_code.address +
                                "::" +
                                inner.token_code.module +
                                "::" +
                                inner.token_code.name;
                        holders.add(new AddressHolder(eventAddress, sb));
                    } catch (DeserializationError deserializationError) {
                        logger.error("decode event data error:", deserializationError);
                    }
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

    static class AddressHolder {
        private final String address;
        private final String tokenCode;

        AddressHolder(String address, String tokenCode) {
            this.address = address;
            this.tokenCode = tokenCode;
        }
    }
}
