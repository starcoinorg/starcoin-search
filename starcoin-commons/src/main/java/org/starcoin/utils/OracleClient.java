package org.starcoin.utils;

import com.alibaba.fastjson.JSON;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.starcoin.bean.OracleTokenPair;

import java.io.IOException;
import java.util.List;

public class OracleClient {
    private String scheme;
    private String host;

    public OracleClient(String scheme, String host) {
        this.scheme = scheme;
        this.host = host;
    }

    public List<OracleTokenPair> getOracleTokenPair(String network) throws IOException {
        OkHttpClient client = new OkHttpClient();
        HttpUrl httpUrl = new HttpUrl.Builder()
                .scheme(scheme)
                .host(host)
                .addPathSegment(network)
                .addPathSegment("v1")
                .addPathSegment("priceFeeds")
                .build();
        Request request = new Request.Builder()
                .url(httpUrl)
                .build();

        try (Response response = client.newCall(request).execute()) {
            assert response.body() != null;
            String res = response.body().string();
            return JSON.parseArray(res, OracleTokenPair.class);
        }
    }

    public OracleTokenPair getProximatePriceRound(String network, String pairId, String timestamp) throws IOException {
        OkHttpClient client = new OkHttpClient();
        HttpUrl httpUrl = new HttpUrl.Builder()
                .scheme(scheme)
                .host(host)
                .addPathSegment(network)
                .addPathSegment("v1")
                .addPathSegment("getProximatePriceRound")
                .addQueryParameter("pairId", pairId)
                .addQueryParameter("timestamp", timestamp)
                .build();
        Request request = new Request.Builder()
                .url(httpUrl)
                .build();

        try (Response response = client.newCall(request).execute()) {
            assert response.body() != null;
            String res = response.body().string();
            return JSON.parseObject(res, OracleTokenPair.class);
        }
    }
}
