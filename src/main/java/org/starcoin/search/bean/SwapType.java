package org.starcoin.search.bean;

public enum SwapType {
    AddLiquidity("add_liquidity"),
    SwapExactTokenForToken("swap_exact_token_for_token"),
    SwapTokenForExactToken("swap_token_for_exact_token"),
    RemoveLiquidity("remove_liquidity"),
    Unknown("unknown");
    String value;

    SwapType(String value) {
        this.value = value;
    }

    public static SwapType fromValue(String value) {
        for (SwapType type : SwapType.values()) {
            if (type.getValue().equals(value)) {
                return type;
            }
        }
        return SwapType.Unknown; //not found
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return this.value;
    }
}
