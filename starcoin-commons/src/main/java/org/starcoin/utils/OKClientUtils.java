package org.starcoin.utils;

import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;

import java.util.concurrent.TimeUnit;

public class OKClientUtils {
    public static OkHttpClient getClient() {
        return new OkHttpClient().newBuilder()
                .connectionPool(pool())
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build();
    }

    private static ConnectionPool pool() {
        return new ConnectionPool(60, 600, TimeUnit.SECONDS);
    }

}
