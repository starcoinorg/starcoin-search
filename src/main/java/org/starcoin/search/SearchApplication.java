package org.starcoin.search;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.starcoin.api.*;
import org.starcoin.search.handler.ElasticSearchHandler;
import org.starcoin.search.handler.RepairHandle;
import org.starcoin.search.utils.SwapApiClient;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

@SpringBootApplication
public class SearchApplication {
    private static Logger logger = LoggerFactory.getLogger(SearchApplication.class);
    @Value("${starcoin.swap.api.url}")
    private String swapApiUrl;

    public static void main(String[] args) {
        Map<String, String> envMap = System.getenv();
        String progArgs = envMap.get("PROG_ARGS");
        logger.info("PROG_ARGS: {}", progArgs);
        if (progArgs != null && progArgs.length() > 0) {
            //set env to args for docker environment
            String[] progs = progArgs.split(" ");
            int i = 0;
            for (String prog : progs) {
                args[i] = prog;
                i++;
            }
        }
        ConfigurableApplicationContext context = SpringApplication.run(SearchApplication.class, args);
        if (args != null && args.length >= 2) {
            RepairHandle repairHandle = (RepairHandle) context.getBean("repairHandle");
            if (args[0].equals("repair")) {
                long blockNumber = Long.parseLong(args[1]);
                repairHandle.repair(blockNumber);
            }
            if (args[0].equals("check")) {
                long begin = Long.parseLong(args[1]);
                long end = Long.parseLong(args[2]);
                repairHandle.check(begin, end);
            }
            if (args[0].equals("repair_file")) {
                try {
                    BufferedReader in = new BufferedReader(new FileReader(args[1]));
                    String str;
                    long blockNumber = 0;
                    while ((str = in.readLine()) != null) {
                        blockNumber = Long.parseLong(str);
                        if (blockNumber > 0) {
                            repairHandle.repair(blockNumber);
                            logger.info("repair ok :" + str);
                        }
                    }
                    logger.info("repair done");
                } catch (IOException e) {
                    logger.error("repair file error:", e);
                }
            }
            if (args[0].equals("auto_repair")) {
                int count = 20;
                long startNumber = Long.parseLong(args[1]);
                while (startNumber > 0) {
                    if (repairHandle.autoRepair(startNumber, count)) {
                        startNumber += count;
                    }
                    try {
                        Thread.currentThread().sleep(2000);
                    } catch (InterruptedException e) {
                        logger.error("auto repair error:", e);
                    }
                }
            }
            if (args[0].equals("add_token")) {
                ElasticSearchHandler elasticSearchHandler = (ElasticSearchHandler) context.getBean("elasticSearchHandler");
                elasticSearchHandler.insertToken(args[1]);
            }
            //update mapping for add deleted tag
            if (args[0].equals("update_mapping")) {
                ElasticSearchHandler elasticSearchHandler = (ElasticSearchHandler) context.getBean("elasticSearchHandler");
                elasticSearchHandler.updateMapping();
            }
        }
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

    @Bean
    StateRPCClient stateRPCClient(URL baseUrl) {
        return new StateRPCClient(baseUrl);
    }

    @Bean
    TokenContractRPCClient tokenContractRPCClient(URL baseUrl) {
        return new TokenContractRPCClient(baseUrl);
    }

    @Bean
    ContractRPCClient contractRPCClient(URL baseUrl) {
        return new ContractRPCClient(baseUrl);
    }

    @Bean
    SwapApiClient swapApiClient() {
        try {
            URL swapUrl = new URL(swapApiUrl);
            return new SwapApiClient(swapUrl.getProtocol(), swapUrl.getHost());
        } catch (MalformedURLException e) {
            logger.error("get swap api url error:", e);
        }
        return null;
    }
}