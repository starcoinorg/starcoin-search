package org.starcoin.utils;

import org.starcoin.constant.StarcoinNetwork;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TokenUtils {
    ///cache all network tokens
    private static Map<StarcoinNetwork, Map<String, String>> TOKEN_CACHE = new HashMap<>();

    public static String toShort(String token) {
        if (token.contains("::")) {
            String[] tokenSplits = token.split("::");
            if (tokenSplits.length == 3) {
                return tokenSplits[2];
            }
        }
        return token;
    }

    public static String toLong(String network, String token) {
        if (token.contains("::")) {
            return token;
        }
        return getLongTokenFromCache(network, token);
    }

    public static void putCache(String network, List<String> longTokens) {
        if (network == null || longTokens == null || longTokens.size() < 1) {
            return;
        }
        StarcoinNetwork starcoinNetwork = StarcoinNetwork.fromValue(network);
        //long token list to map
        Map<String, String> tokenMap = new HashMap<>();
        for (String longToken : longTokens) {
            tokenMap.put(toShort(longToken), longToken);
        }
        TOKEN_CACHE.put(starcoinNetwork, tokenMap);
    }

    public static String getLongTokenFromCache(String network, String shortToken) {
        if (network == null || network.length() < 1 || shortToken == null || shortToken.length() < 1) {
            return null;
        }
        StarcoinNetwork starcoinNetwork = StarcoinNetwork.fromValue(network);
        Map<String, String> tokenMap = TOKEN_CACHE.get(starcoinNetwork);
        if (tokenMap != null) {
            return tokenMap.get(shortToken);
        }
        return null;
    }
}
