package org.starcoin.scan.service;

import org.junit.jupiter.api.Test;
import org.starcoin.utils.KeyUtils;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

class RateLimitServiceTest {

    @Test
    void generateApiKey() {
        String key = KeyUtils.base62Encode();
        System.out.println(key.toUpperCase(Locale.ROOT));
    }
}