package org.starcoin.scan.service.vo;

public class DIAppConfigVo {
    String starcoinVersion;
    String processingVersion;
    String network;
    String apiVersion;
    String webVersion;

    public String getStarcoinVersion() {
        return starcoinVersion;
    }

    public void setStarcoinVersion(String starcoinVersion) {
        this.starcoinVersion = starcoinVersion;
    }

    public String getProcessingVersion() {
        return processingVersion;
    }

    public void setProcessingVersion(String processingVersion) {
        this.processingVersion = processingVersion;
    }

    public String getNetwork() {
        return network;
    }

    public void setNetwork(String network) {
        this.network = network;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    public String getWebVersion() {
        return webVersion;
    }

    public void setWebVersion(String webVersion) {
        this.webVersion = webVersion;
    }
}
