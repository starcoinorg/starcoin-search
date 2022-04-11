package org.starcoin.indexer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.starcoin.api.*;
import org.starcoin.indexer.handler.*;
import org.starcoin.utils.SwapApiClient;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import static org.starcoin.utils.DateTimeUtils.getTimeStamp;

@SpringBootApplication(scanBasePackages = "org.starcoin")
@EntityScan("org.starcoin.bean")
public class IndexerApplication {
    private static final Logger logger = LoggerFactory.getLogger(IndexerApplication.class);
    @Value("${starcoin.swap.api.url}")
    private String swapAPIUrl;

    public static void main(String[] args) {
        Map<String, String> envMap = System.getenv();
        String programArgs = envMap.get("PROGRAM_ARGS");
        logger.info("PROGRAM_ARGS: {}", programArgs);
        if (programArgs != null && programArgs.length() > 0) {
            //set env to args for docker environment
            String[] pros = programArgs.split(" ");
            int i = 0;
            for (String arg : pros) {
                args[i] = arg;
                i++;
            }
        }
        ConfigurableApplicationContext context = SpringApplication.run(IndexerApplication.class, args);
        if (args == null || args.length < 1) {
            logger.warn("arg is null.");
            return;
        }

        ElasticSearchHandler elasticSearchHandler = (ElasticSearchHandler) context.getBean("elasticSearchHandler");
        //start subscribe event handle
        if (args[0].equals("subscribe")) {
            String hosts = context.getEnvironment().getProperty("HOSTS");
            if (hosts != null && hosts.length() > 0) {
                String[] seeds = hosts.split(",");
                for (String seed : seeds) {
                    Thread handlerThread = new Thread(new SubscribeHandler(seed, elasticSearchHandler));
                    handlerThread.start();
                }
                logger.info("subscribe event handle start ok.");
            }
        }
        //add token info
        if (args[0].equals("add_token")) {
            elasticSearchHandler.insertToken(args[1]);
        }
        //update mapping for add deleted tag
        if (args[0].equals("update_mapping")) {
            elasticSearchHandler.updateMapping();
        }
        //holder history rebuild
        if (args[0].equals("holder")) {
            HolderHistoryHandle handle = (HolderHistoryHandle) context.getBean("holderHistoryHandle");
            handle.handle();
        }

        //repair block data
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
                long blockNumber;
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
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    logger.error("auto repair error:", e);
                }
            }
        }
        //swap handle
        if (args[0].equals("swap_handle")) {
            SwapHandle swapHandle = (SwapHandle) context.getBean("swapHandle");
            int beginDate = Integer.parseInt(args[1]);
            int endDate = Integer.parseInt(args[2]);
            for (int i = beginDate; i < endDate; i++) {
                long startTs = getTimeStamp(i - 1);
                long endTs = getTimeStamp(i);
                swapHandle.swapStat(startTs, endTs);
                logger.info("swap index repair ok: {} , {}", startTs, endTs);
            }
        }
        //save price list
        if(args[0].equals("token_price_handle")) {
            TokenPriceHandle tokenPriceHandle = (TokenPriceHandle) context.getBean("tokenPriceHandle");
            if("get".equals(args[1])) {
                int date = Integer.parseInt(args[2]);
                tokenPriceHandle.getPrice(date);
            }
            if("stat".equals(args[1])) {
                int date = Integer.parseInt(args[2]);
                tokenPriceHandle.statPrice(date);
            }
            if("repair".equals(args[1])) {
                int beginDate = Integer.parseInt(args[2]);
                int endDate = Integer.parseInt(args[3]);
                for (int i = beginDate; i < endDate; i++) {
                    tokenPriceHandle.getPrice(i);
                    logger.info("swap txn repair ok: {} ", i);
                }
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
            URL swapUrl = new URL(swapAPIUrl);
            return new SwapApiClient(swapUrl.getProtocol(), swapUrl.getHost());
        } catch (MalformedURLException e) {
            logger.error("get swap api url error:", e);
        }
        return null;
    }
}