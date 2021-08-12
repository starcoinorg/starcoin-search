package org.starcoin.search;

import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.starcoin.api.BlockRPCClient;
import org.starcoin.api.StateRPCClient;
import org.starcoin.api.TokenContractRPCClient;
import org.starcoin.api.TransactionRPCClient;
import org.starcoin.search.handler.RepairHandle;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

@SpringBootApplication
public class SearchApplication {

    private static Logger logger = LoggerFactory.getLogger(SearchApplication.class);

    public static void main(String[] args) {
        logger.info("start search service...");
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
                            System.out.println("repair ok :" + str);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            //default mode
            Scheduler scheduler = (Scheduler) context.getBean("scheduler");
            if (scheduler != null) {
                try {
                    scheduler.start();
                } catch (SchedulerException e) {
                    e.printStackTrace();
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

}