package org.starcoin.search;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.starcoin.api.BlockRPCClient;
import org.starcoin.api.TransactionRPCClient;
import org.starcoin.search.handler.ElasticSearchHandler;

import java.net.MalformedURLException;
import java.net.URL;

@SpringBootApplication
public class SearchApplication {

    private static Logger logger = LoggerFactory.getLogger(SearchApplication.class);

    @Autowired
    private ElasticSearchHandler elasticSearchHandler;

    public static void main(String[] args) {
        logger.info("start search service...");
        SpringApplication.run(SearchApplication.class, args);
        logger.info("APPLICATION FINISHED");
    }

    @Bean(name = "base_url")
    URL baseUrl(@Value("${starcoin.seeds}") String host) {
        try {
            return new URL("http://" + host + ":9850");
        } catch (MalformedURLException e) {
            logger.error("get base url error:", e);
        }
        return null;
    }

    @Bean
    TransactionRPCClient transactionRPCClient(URL baseUrl) {
        return new TransactionRPCClient(baseUrl);
    }

    @Bean
    BlockRPCClient blockRPCClient(URL baseUrl) {
        return new BlockRPCClient(baseUrl);
    }

}