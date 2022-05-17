package org.starcoin.bean;

import com.vladmihalcea.hibernate.type.json.JsonType;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "transaction_payload")
@TypeDef(name = "json", typeClass = JsonType.class)
public class TransactionPayload {
    @Id
    @Column(name = "transaction_hash")
    private String transactionHash;
    @Type(type = "json")
    @Column(name = "json_val", columnDefinition = "json")
    private String json;

    public String getTransactionHash() {
        return transactionHash;
    }

    public void setTransactionHash(String transactionHash) {
        this.transactionHash = transactionHash;
    }

    public String getJson() {
        return json;
    }

    public void setJson(String json) {
        this.json = json;
    }
}
