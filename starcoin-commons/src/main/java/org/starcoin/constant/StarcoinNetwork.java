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
        for (StarcoinNetwork type : StarcoinNetwork.values()) {
            if (type.getValue().equals(value)) {
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
