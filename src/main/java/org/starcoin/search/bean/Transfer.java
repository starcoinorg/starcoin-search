package org.starcoin.search.bean;

import com.alibaba.fastjson.annotation.JSONField;

public class Transfer {

    long timestamp;

    @JSONField(name = "txn_hash")
    String txnHash;

    @JSONField(name = "block_number")
    String blockNumber;

}
