package org.starcoin.search.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.novi.serde.DeserializationError;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.starcoin.bean.Transaction;
import org.starcoin.search.bean.Offset;
import org.starcoin.search.constant.Constant;
import org.starcoin.types.*;
import org.starcoin.utils.*;

import java.io.IOException;
import java.util.List;

public class TransactionPayloadHandle extends QuartzJobBean {

    private static Logger logger = LoggerFactory.getLogger(TransactionPayloadHandle.class);

    private ObjectMapper objectMapper;

    public TransactionPayloadHandle() {
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
    }

    @Autowired
    private ElasticSearchHandler elasticSearchHandler;

    @Value("${starcoin.network}")
    private String network;

    public ElasticSearchHandler getElasticSearchHandler() {
        return elasticSearchHandler;
    }

    public void setElasticSearchHandler(ElasticSearchHandler elasticSearchHandler) {
        this.elasticSearchHandler = elasticSearchHandler;
    }

    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        Offset transactionPayloadRemoteOffset = elasticSearchHandler.getRemoteOffset(Constant.PAYLOAD_INDEX);

        try {
            List<Transaction> transactionList = elasticSearchHandler.getByTimestamp(network, transactionPayloadRemoteOffset.getBlockHeight());
            elasticSearchHandler.bulkAddPayload(transactionList, objectMapper);
            Offset currentOffset = new Offset(transactionList.get(transactionList.size() - 1).getTimestamp(), null);
            elasticSearchHandler.setRemoteOffset(currentOffset, Constant.PAYLOAD_INDEX);
        } catch (IOException | DeserializationError e) {
            logger.warn("query es failed .", e);
        }
    }

}
