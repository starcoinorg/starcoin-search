package org.starcoin.bean;

public enum TransactionQueryType {
    ALL(0),
    Script(1),
    Package(2),
    ScriptFunction(3);
    int value;

    TransactionQueryType(int value) {
        this.value = value;
    }

    public static TransactionQueryType fromValue(int value) {
        for (TransactionQueryType type : TransactionQueryType.values()) {
            if (type.getValue() == value) {
                return type;
            }
        }
        return TransactionQueryType.ALL;
    }

    public TransactionType toTransactionType() {
        if (this.equals(TransactionQueryType.Script)) {
            return TransactionType.Script;
        } else if (this.equals(TransactionQueryType.Package)) {
            return TransactionType.Package;
        } else if (this.equals(TransactionQueryType.ScriptFunction)) {
            return TransactionType.ScriptFunction;
        }
        //todo
        return TransactionType.ScriptFunction;
    }


    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }
}
