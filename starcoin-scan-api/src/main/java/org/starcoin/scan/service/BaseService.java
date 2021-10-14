package org.starcoin.scan.service;

import org.springframework.beans.factory.annotation.Value;


public class BaseService {

    @Value("${indexer.version}")
    private String indexVersion;

    public String getIndex(String network, String indexConstant) {
        return network + indexVersion + "." + indexConstant;
    }
}
