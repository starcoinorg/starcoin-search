package org.starcoin.search.utils;

import com.alibaba.fastjson.JSON;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.starcoin.search.bean.LiquidityPoolInfo;
import org.starcoin.search.bean.OracleTokenPair;
import org.starcoin.search.bean.SwapToken;
import org.starcoin.search.bean.TokenTvl;

import java.io.IOException;
import java.util.List;

public class SwapApiClient {
    private String scheme;
    private String host;

    public SwapApiClient(String scheme, String host) {
        this.scheme = scheme;
        this.host = host;
    }

    public List<SwapToken> getTokens(String network) throws IOException {
        OkHttpClient client = new OkHttpClient();
        HttpUrl httpUrl = new HttpUrl.Builder()
                .scheme(scheme)
                .host(host)
                .addPathSegment(network)
                .addPathSegment("v1")
                .addPathSegment("tokens")
                .build();
        Request request = new Request.Builder()
                .url(httpUrl)
                .build();

        try (Response response = client.newCall(request).execute()) {
            String res = response.body().string();
            return JSON.parseArray(res, SwapToken.class);
        }
    }

    public List<LiquidityPoolInfo> getPoolInfo(String network) throws IOException {
        OkHttpClient client = new OkHttpClient();
        HttpUrl httpUrl = new HttpUrl.Builder()
                .scheme(scheme)
                .host(host)
                .addPathSegment(network)
                .addPathSegment("v1")
                .addPathSegment("liquidityPools")
                .build();
        Request request = new Request.Builder()
                .url(httpUrl)
                .build();

        try (Response response = client.newCall(request).execute()) {
            String res = response.body().string();
            return JSON.parseArray(res, LiquidityPoolInfo.class);
        }
    }

    public OracleTokenPair getProximatePriceRound(String network, String token, String timestamp) throws IOException {
        OkHttpClient client = new OkHttpClient();
        HttpUrl httpUrl = new HttpUrl.Builder()
                .scheme(scheme)
                .host(host)
                .addPathSegment(network)
                .addPathSegment("v1")
                .addPathSegment("getProximateToUsdPriceRound")
                .addQueryParameter("token", token)
                .addQueryParameter("timestamp", timestamp)
                .build();
        Request request = new Request.Builder()
                .url(httpUrl)
                .build();

        try (Response response = client.newCall(request).execute()) {
            String res = response.body().string();
            return JSON.parseObject(res, OracleTokenPair.class);
        }
    }

    public List<OracleTokenPair> getProximatePriceRounds(String network, List<String> tokenList, String timestamp) throws IOException {
        OkHttpClient client = new OkHttpClient();
        HttpUrl.Builder builder = new HttpUrl.Builder()
                .scheme(scheme)
                .host(host)
                .addPathSegment(network)
                .addPathSegment("v1")
                .addPathSegment("price-api")
                .addPathSegment("getProximateToUsdPriceRounds")
                .addQueryParameter("timestamp", timestamp);
        for(String token : tokenList) {
            builder.addQueryParameter("t", token);
        }
        HttpUrl httpUrl = builder.build();
        Request request = new Request.Builder()
                .url(httpUrl)
                .build();

        System.out.println(request.toString());
        try (Response response = client.newCall(request).execute()) {
            String res = response.body().string();
            return JSON.parseArray(res, OracleTokenPair.class);
        }
    }

    public List<TokenTvl> getTokenTvl(String network) throws IOException {
        OkHttpClient client = new OkHttpClient();
        HttpUrl httpUrl = new HttpUrl.Builder()
                .scheme(scheme)
                .host(host)
                .addPathSegment(network)
                .addPathSegment("v1")
                .addPathSegment("sumReservesGroupByToken")
                .build();
        Request request = new Request.Builder()
                .url(httpUrl)
                .build();

        try (Response response = client.newCall(request).execute()) {
            String res = response.body().string();
            return JSON.parseArray(res, TokenTvl.class);
        }
    }

}
