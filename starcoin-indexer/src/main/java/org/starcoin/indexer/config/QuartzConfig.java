package org.starcoin.indexer.config;

import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.starcoin.config.SearchJobFactory;
import org.starcoin.indexer.handler.*;

import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

@Configuration
public class QuartzConfig {
    @Value("${starcoin.bg_task.jobs}")
    private String[] jobs;

    @Autowired
    private SearchJobFactory searchJobFactory;

    @Bean
    public JobDetail indexerJob() {
        return JobBuilder.newJob(IndexerHandle.class).withIdentity("indexer").storeDurably().build();
    }

    @Bean
    public Trigger indexerTrigger() {
        SimpleScheduleBuilder scheduleBuilder = SimpleScheduleBuilder.simpleSchedule()
                .withIntervalInSeconds(10)  //设置时间周期单位秒
                .repeatForever();
        return TriggerBuilder.newTrigger().forJob(indexerJob())
                .withIdentity("indexer")
                .withSchedule(scheduleBuilder)
                .build();
    }

    @Bean
    public JobDetail secondaryJob() {
        return JobBuilder.newJob(SecondaryIndexer.class).withIdentity("secondary").storeDurably().build();
    }

    @Bean
    public Trigger secondaryTrigger() {
        SimpleScheduleBuilder scheduleBuilder = SimpleScheduleBuilder.simpleSchedule()
                .withIntervalInSeconds(10)  //设置时间周期单位秒
                .repeatForever();
        return TriggerBuilder.newTrigger().forJob(secondaryJob())
                .withIdentity("secondary")
                .withSchedule(scheduleBuilder)
                .build();
    }

    @Bean
    public JobDetail MarketCapJob() {
        return JobBuilder.newJob(MarketCapIndexer.class).withIdentity("market_cap").storeDurably().build();
    }

    @Bean
    public Trigger marketCapTrigger() {
        SimpleScheduleBuilder scheduleBuilder = SimpleScheduleBuilder.simpleSchedule()
                .withIntervalInHours(24)
                .repeatForever();
        return TriggerBuilder.newTrigger().forJob(MarketCapJob())
                .withIdentity("market_cap")
                .withSchedule(scheduleBuilder)
                .build();
    }

    @Bean
    public JobDetail transactionPayloadJob() {
        return JobBuilder.newJob(TransactionPayloadHandle.class).withIdentity("txn_payload").storeDurably().build();
    }

    @Bean
    public Trigger transactionPayloadTrigger() {
        SimpleScheduleBuilder scheduleBuilder = SimpleScheduleBuilder.simpleSchedule()
                .withIntervalInSeconds(10)
                .repeatForever();
        return TriggerBuilder.newTrigger().forJob(transactionPayloadJob())
                .withIdentity("txn_payload")
                .withSchedule(scheduleBuilder)
                .build();
    }

    @Bean
    public JobDetail swapStatsJob() {
        return JobBuilder.newJob(SwapIndexer.class).withIdentity("swap_stats").storeDurably().build();
    }

    @Bean
    public Trigger swapStatsTrigger() {
        SimpleScheduleBuilder scheduleBuilder = SimpleScheduleBuilder.simpleSchedule()
                .withIntervalInHours(6)
                .repeatForever();
        return TriggerBuilder.newTrigger().forJob(swapStatsJob())
                .withIdentity("swap_stats")
                .withSchedule(scheduleBuilder)
                .build();
    }

    @Bean
    public JobDetail cleanPendingTxnJob() {
        return JobBuilder.newJob(PendingTxnCleanHandle.class).withIdentity("clean_pending_txn").storeDurably().build();
    }

    @Bean
    public Trigger cleanPendingTxnTrigger() {
        SimpleScheduleBuilder scheduleBuilder = SimpleScheduleBuilder.simpleSchedule()
                .withIntervalInSeconds(20)  //设置时间周期单位秒
                .repeatForever();
        return TriggerBuilder.newTrigger().forJob(cleanPendingTxnJob())
                .withIdentity("clean_pending_txn")
                .withSchedule(scheduleBuilder)
                .build();
    }

    //txn global index update
    @Bean
    public JobDetail txnGlobalIndexUpdateJob() {
        return JobBuilder.newJob(TransactionInfoIndexer.class).withIdentity("txn_global_idx_update").storeDurably().build();
    }

    @Bean
    public Trigger txnGlobalIndexUpdateTrigger() {
        SimpleScheduleBuilder scheduleBuilder = SimpleScheduleBuilder.simpleSchedule()
                .withIntervalInSeconds(2)  //设置时间周期单位秒
                .repeatForever();
        return TriggerBuilder.newTrigger().forJob(txnGlobalIndexUpdateJob())
                .withIdentity("txn_global_idx_update")
                .withSchedule(scheduleBuilder)
                .build();
    }

    // swap event handle
    @Bean
    public JobDetail swapEventHandleJob() {
        return JobBuilder.newJob(SwapEventIndexer.class).withIdentity("swap_event_handle").storeDurably().build();
    }

    @Bean
    public Trigger swapEventHandleTrigger() {
        SimpleScheduleBuilder scheduleBuilder = SimpleScheduleBuilder.simpleSchedule()
                .withIntervalInSeconds(5)  //设置时间周期单位秒
                .repeatForever();
        return TriggerBuilder.newTrigger().forJob(swapEventHandleJob())
                .withIdentity("swap_event_handle")
                .withSchedule(scheduleBuilder)
                .build();
    }

    // swap pool fee stat handle
    @Bean
    public JobDetail swapPoolFeeStatJob() {
        return JobBuilder.newJob(SwapPoolFeeStatIndexer.class).withIdentity("swap_pool_fee_stat").storeDurably().build();
    }

    @Bean
    public Trigger swapPoolFeeStatTrigger() {
        SimpleScheduleBuilder scheduleBuilder = SimpleScheduleBuilder.simpleSchedule()
                .withIntervalInHours(12)
                .repeatForever();
        return TriggerBuilder.newTrigger().forJob(swapPoolFeeStatJob())
                .withIdentity("swap_pool_fee_stat")
                .withSchedule(scheduleBuilder)
                .build();
    }

    // token price hour handle
    @Bean
    public JobDetail priceHourJob() {
        return JobBuilder.newJob(TokenPriceHourIndexer.class).withIdentity("price_hour").storeDurably().build();
    }

    @Bean
    public Trigger priceHourTrigger() {
        SimpleScheduleBuilder scheduleBuilder = SimpleScheduleBuilder.simpleSchedule()
                .withIntervalInHours(1)
                .repeatForever();
        return TriggerBuilder.newTrigger().forJob(priceHourJob())
                .withIdentity("price_hour")
                .withSchedule(scheduleBuilder)
                .build();
    }

    // token price hour handle
    @Bean
    public JobDetail priceStatJob() {
        return JobBuilder.newJob(TokenPriceStatIndexer.class).withIdentity("price_stat").storeDurably().build();
    }

    @Bean
    public Trigger priceStatTrigger() {
        SimpleScheduleBuilder scheduleBuilder = SimpleScheduleBuilder.simpleSchedule()
                .withIntervalInHours(1)
                .repeatForever();
        return TriggerBuilder.newTrigger().forJob(priceStatJob())
                .withIdentity("price_stat")
                .withSchedule(scheduleBuilder)
                .build();
    }

    @Bean
    public SchedulerFactoryBean schedulerFactoryBean() {
        SchedulerFactoryBean schedulerFactoryBean = new SchedulerFactoryBean();
        try {
            schedulerFactoryBean.setQuartzProperties(quartzProperties());
            schedulerFactoryBean.setJobFactory(searchJobFactory);
            schedulerFactoryBean.setAutoStartup(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return schedulerFactoryBean;
    }

    private Properties quartzProperties() {
        Properties prop = new Properties();
        prop.put("org.quartz.scheduler.instanceName", "quartzScheduler");// 调度器的实例名
        prop.put("org.quartz.scheduler.instanceId", "AUTO");// 实例的标识
        prop.put("org.quartz.scheduler.skipUpdateCheck", "true");// 检查quartz是否有版本更新（true 不检查）
        prop.put("org.quartz.threadPool.class", "org.quartz.simpl.SimpleThreadPool");// 线程池的名字
        prop.put("org.quartz.threadPool.threadCount", "1");// 指定线程数量
        prop.put("org.quartz.threadPool.threadPriority", "5");// 线程优先级（1-10）默认为5
        prop.put("org.quartz.threadPool.threadsInheritContextClassLoaderOfInitializingThread", "true");
        return prop;
    }

    @Bean
    public Scheduler scheduler() throws SchedulerException {
        Scheduler scheduler = schedulerFactoryBean().getScheduler();
        if (jobs == null || jobs.length < 1) {
            return scheduler;
        }
        Set<String> jobSet = new HashSet<>();
        Collections.addAll(jobSet, jobs);

        JobDetail job = indexerJob();
        if (jobSet.contains(job.getKey().getName())) {
            scheduler.scheduleJob(job, indexerTrigger());
        }
        job = secondaryJob();
        if (jobSet.contains(job.getKey().getName())) {
            scheduler.scheduleJob(job, secondaryTrigger());
        }
        job = MarketCapJob();
        if (jobSet.contains(job.getKey().getName())) {
            scheduler.scheduleJob(job, marketCapTrigger());
        }
        job = transactionPayloadJob();
        if (jobSet.contains(job.getKey().getName())) {
            scheduler.scheduleJob(job, transactionPayloadTrigger());
        }
        job = swapStatsJob();
        if (jobSet.contains(job.getKey().getName())) {
            scheduler.scheduleJob(job, swapStatsTrigger());
        }
        job = cleanPendingTxnJob();
        if (jobSet.contains(job.getKey().getName())) {
            scheduler.scheduleJob(job, cleanPendingTxnTrigger());
        }
        job = txnGlobalIndexUpdateJob();
        if (jobSet.contains(job.getKey().getName())) {
            scheduler.scheduleJob(job, txnGlobalIndexUpdateTrigger());
        }
        job = swapEventHandleJob();
        if (jobSet.contains(job.getKey().getName())) {
            scheduler.scheduleJob(job, swapEventHandleTrigger());
        }
        job = swapPoolFeeStatJob();
        if (jobSet.contains(job.getKey().getName())) {
            scheduler.scheduleJob(job, swapPoolFeeStatTrigger());
        }
        job = priceHourJob();
        if (jobSet.contains(job.getKey().getName())) {
            scheduler.scheduleJob(job, priceHourTrigger());
        }
        job = priceStatJob();
        if (jobSet.contains(job.getKey().getName())) {
            scheduler.scheduleJob(job, priceStatTrigger());
        }

        scheduler.start();
        return scheduler;
    }
}