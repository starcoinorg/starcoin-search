package org.starcoin.utils;

import java.util.function.Function;

@FunctionalInterface
public interface ExceptionWrap<T, R> {
    R apply(T t) throws Exception;

    static <T, R> Function<T, R> wrap(ExceptionWrap<T, R> checkedFunction) {
        return t -> {
            try {
                return checkedFunction.apply(t);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }
}
