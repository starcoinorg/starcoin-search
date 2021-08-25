package org.starcoin.search.config;

import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.starcoin.search.handler.IndexerHandle;
import org.starcoin.search.handler.MarketCapIndexer;
import org.starcoin.search.handler.SecondaryIndexer;
import org.starcoin.search.handler.TransactionPayloadHandle;

import java.util.Properties;

@Configuration
public class QuartzConfig {
    @Value("${starcoin.indexer.auto_start}")
    private boolean autoStart;

    @Autowired
    private SearchJobFactory searchJobFactory;

    @Bean
    public JobDetail handleIndexer() {
        return JobBuilder.newJob(IndexerHandle.class).withIdentity("indexer").storeDurably().build();
    }

    @Bean
    public JobDetail handleSecondIndexer() {
        return JobBuilder.newJob(SecondaryIndexer.class).withIdentity("secondary").storeDurably().build();
    }

    @Bean
    public JobDetail handleMarketCapIndexer() {
        return JobBuilder.newJob(MarketCapIndexer.class).withIdentity("market").storeDurably().build();
    }

    @Bean
    public JobDetail handleTransactionPayload() {
        return JobBuilder.newJob(TransactionPayloadHandle.class).withIdentity("txn_payload").storeDurably().build();
    }

    @Bean
    public Trigger startQuartzTrigger() {
        SimpleScheduleBuilder scheduleBuilder = SimpleScheduleBuilder.simpleSchedule()
                .withIntervalInSeconds(20)  //设置时间周期单位秒
                .repeatForever();
        return TriggerBuilder.newTrigger().forJob(handleIndexer())
                .withIdentity("indexer")
                .withSchedule(scheduleBuilder)
                .build();
    }

    @Bean
    public Trigger startSecondTrigger() {
        SimpleScheduleBuilder scheduleBuilder = SimpleScheduleBuilder.simpleSchedule()
                .withIntervalInSeconds(30)  //设置时间周期单位秒
                .repeatForever();
        return TriggerBuilder.newTrigger().forJob(handleSecondIndexer())
                .withIdentity("secondary")
                .withSchedule(scheduleBuilder)
                .build();
    }

    @Bean
    public Trigger startMarketCapTrigger() {
        SimpleScheduleBuilder scheduleBuilder = SimpleScheduleBuilder.simpleSchedule()
                .withIntervalInHours(24)
                .repeatForever();
        return TriggerBuilder.newTrigger().forJob(handleMarketCapIndexer())
                .withIdentity("market")
                .withSchedule(scheduleBuilder)
                .build();
    }

    @Bean
    public Trigger startTransactionPayload() {
        SimpleScheduleBuilder scheduleBuilder = SimpleScheduleBuilder.simpleSchedule()
                .withIntervalInSeconds(1)
                .repeatForever();
        return TriggerBuilder.newTrigger().forJob(handleTransactionPayload())
                .withIdentity("txn_payload")
                .withSchedule(scheduleBuilder)
                .build();
    }

    @Bean
    public SchedulerFactoryBean schedulerFactoryBean() {
        SchedulerFactoryBean schedulerFactoryBean = new SchedulerFactoryBean();
        try {
            schedulerFactoryBean.setQuartzProperties(quartzProperties());
            schedulerFactoryBean.setJobFactory(searchJobFactory);
            schedulerFactoryBean.setAutoStartup(autoStart);
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
        scheduler.scheduleJob(handleIndexer(), startQuartzTrigger());
        scheduler.scheduleJob(handleSecondIndexer(), startSecondTrigger());
        scheduler.scheduleJob(handleMarketCapIndexer(), startMarketCapTrigger());
        scheduler.scheduleJob(handleTransactionPayload(), startTransactionPayload());
        if (autoStart) {
            scheduler.start();
        }
        return scheduler;
    }
}