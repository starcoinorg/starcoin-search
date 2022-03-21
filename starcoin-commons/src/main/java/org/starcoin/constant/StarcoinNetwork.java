package org.starcoin.constant;

public enum StarcoinNetwork {
    main("main"),
    barnard("barnard"),
    halley("halley"),
    proxima("proxima"),
    dev("dev"),
    unknown("unknown");
    String value;

    StarcoinNetwork(String value) {
        this.value = value;
    }

    public static StarcoinNetwork fromValue(String value) {
        String networkStr = value;
        if(value != null && value.contains(".")) {
            networkStr = value.substring(0, value.indexOf("."));
        }
        for (StarcoinNetwork type : StarcoinNetwork.values()) {
            if (type.getValue().equals(networkStr)) {
                return type;
            }
        }
        return StarcoinNetwork.unknown; //not found
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
