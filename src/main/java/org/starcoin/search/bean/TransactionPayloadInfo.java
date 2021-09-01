package org.starcoin.search.bean;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.starcoin.types.TransactionPayload;

public class TransactionPayloadInfo {

    private TransactionPayload payload;

    private long timestamp;

    @JsonProperty("transaction_hash")
    String transactionHash;

    public TransactionPayloadInfo(TransactionPayload payload, long timestamp, String transactionHash) {
        this.payload = payload;
        this.timestamp = timestamp;
        this.transactionHash = transactionHash;
    }

    public TransactionPayload getPayload() {
        return payload;
    }

    public void setPayload(TransactionPayload payload) {
        this.payload = payload;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getTransactionHash() {
        return transactionHash;
    }

    public void setTransactionHash(String transactionHash) {
        this.transactionHash = transactionHash;
    }
}
