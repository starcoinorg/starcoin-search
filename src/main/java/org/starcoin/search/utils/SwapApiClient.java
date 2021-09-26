package org.starcoin.search.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.starcoin.search.bean.SwapPoolInfo;
import org.starcoin.search.bean.SwapToken;

import java.io.IOException;
import java.util.ArrayList;
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

    public List<SwapPoolInfo> getPoolInfo(String network) throws IOException {
        OkHttpClient client = new OkHttpClient();
        List<SwapPoolInfo> result = new ArrayList<>();
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
            JSONArray jsonArray = JSON.parseArray(res);
            for(int i =0; i < jsonArray.size(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                SwapPoolInfo poolInfo = jsonObject.getJSONObject("liquidityPoolId").getObject("liquidityTokenId", SwapPoolInfo.class);
                result.add(poolInfo);
            }
        }
        return result;
    }
}
