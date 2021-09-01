package org.starcoin.search.bean;

import com.alibaba.fastjson.annotation.JSONField;

public class EventAddress {
    @JSONField(
            name = "_id"
    )
    String id;
    @JSONField(
            name = "event_address"
    )
    String address;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
