package com.team11.insurance.notifications.config;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Configuration for asynchronous notification processing and background recovery (P4-08).
 * Defines the dedicated {@code notificationExecutor} thread pool per SAD §4.1.7 and P4 Contract Section 4.
 */
@Configuration
@EnableAsync
@EnableScheduling
public class NotificationConfig {

    /**
     * Dedicated thread pool for non-blocking notification dispatch.
     * Core pool size: 4, Max pool size: 16, Queue capacity: 100.
     *
     * @return configured {@link Executor} bean named {@code notificationExecutor}
     */
    @Bean(name = "notificationExecutor")
    public Executor notificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("notif-exec-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
