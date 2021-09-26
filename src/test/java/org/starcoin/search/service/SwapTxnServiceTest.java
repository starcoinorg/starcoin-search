package org.starcoin.search.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.starcoin.search.SearchApplication;
import org.starcoin.search.bean.TokenStat;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(
        classes = SearchApplication.class)
@TestPropertySource(
        locations = "classpath:application-integrationtest.properties")
class SwapTxnServiceTest {

    @Autowired
    private SwapTxnService swapTxnService;
    @BeforeEach
    void setUp() {

    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void getTokenVolume() {
        TokenStat tokenStat = swapTxnService.getTokenVolume("0x00000000000000000000000000000001::STC::STC", 1617194769000L, 1617194769001L);
        System.out.println(tokenStat);
        assertNotNull(tokenStat);
    }
}