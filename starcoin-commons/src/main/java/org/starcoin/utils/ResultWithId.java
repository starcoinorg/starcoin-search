package org.starcoin.utils;

import org.starcoin.api.Result;

import java.util.List;

public class ResultWithId<T> extends Result<T> {

    private List<String> ids;

    public List<String> getIds() {
        return ids;
    }

    public void setIds(List<String> ids) {
        this.ids = ids;
    }
}
