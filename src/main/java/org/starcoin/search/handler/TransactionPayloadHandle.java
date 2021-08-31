package org.starcoin.search.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.novi.serde.DeserializationError;
import com.thetransactioncompany.jsonrpc2.client.JSONRPC2SessionException;
import org.elasticsearch.client.RestHighLevelClient;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.starcoin.bean.Transaction;
import org.starcoin.search.bean.TransferOffset;
import org.starcoin.search.constant.Constant;
import org.starcoin.types.*;
import org.starcoin.utils.*;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.List;

public class TransactionPayloadHandle extends QuartzJobBean {

    private static Logger logger = LoggerFactory.getLogger(TransactionPayloadHandle.class);
    private ObjectMapper objectMapper;
    private RestHighLevelClient client;
    private String index;

    @Autowired
    private ElasticSearchHandler elasticSearchHandler;
    @Value("${starcoin.network}")
    private String network;

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
    protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {
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
            List<Transaction> transactionList = elasticSearchHandler.getTransactionByTimestamp(network, transactionPayloadRemoteOffset.getTimestamp());
            if (!transactionList.isEmpty()) {
                elasticSearchHandler.addUserTransactionToList(transactionList);
                elasticSearchHandler.bulkAddPayload(index, transactionList, objectMapper);
                Transaction last = transactionList.get(transactionList.size() - 1);
                TransferOffset currentOffset = new TransferOffset();
                currentOffset.setTimestamp(String.valueOf(last.getTimestamp()));
                ServiceUtils.setRemoteOffset(client, index, currentOffset);
                logger.info("update payload ok: {}", currentOffset);
            } else {
                logger.warn("get txn_info null");
            }

        } catch (IOException | DeserializationError | JSONRPC2SessionException e) {
            logger.warn("query es failed .", e);
        }
    }

}
