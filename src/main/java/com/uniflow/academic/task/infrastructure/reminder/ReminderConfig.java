package com.uniflow.academic.task.infrastructure.reminder;

import com.uniflow.academic.task.application.ports.out.ReminderScheduler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import software.amazon.awssdk.services.scheduler.SchedulerClient;

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableConfigurationProperties(ReminderProperties.class)
public class ReminderConfig {

    static final String EXECUTOR = "reminderExecutor";

    @Bean
    @ConditionalOnProperty(name = "reminder.scheduler", havingValue = "noop", matchIfMissing = true)
    public ReminderScheduler noopReminderScheduler() {
        return new NoopReminderScheduler();
    }

    /** Region and credentials come from the AWS default provider chains (task role on ECS). */
    @Bean
    @ConditionalOnProperty(name = "reminder.scheduler", havingValue = "eventbridge")
    @ConditionalOnMissingBean(SchedulerClient.class)
    public SchedulerClient schedulerClient(ReminderProperties properties) {
        return SchedulerClientFactory.configuredBuilder(properties).build();
    }

    @Bean
    @ConditionalOnProperty(name = "reminder.scheduler", havingValue = "eventbridge")
    public ReminderScheduler eventBridgeReminderScheduler(
            SchedulerClient client, ReminderProperties properties, Clock clock
    ) {
        return new EventBridgeReminderScheduler(
                client,
                clock,
                properties.groupName(),
                properties.queueArn(),
                properties.roleArn(),
                Duration.ofHours(properties.leadTimeHours())
        );
    }

    /**
     * Small bounded pool for post-commit reminder calls. When it is saturated the submission is rejected
     * (never run on the caller) and the reminder is dropped with an error log.
     */
    @Bean(EXECUTOR)
    @ConditionalOnProperty(name = "reminder.async", havingValue = "true", matchIfMissing = true)
    public ThreadPoolTaskExecutor reminderExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("reminder-");
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);
        return executor;
    }

    /** Runs reminder calls on the calling thread; used by tests that assert right after a request. */
    @Bean(EXECUTOR)
    @ConditionalOnProperty(name = "reminder.async", havingValue = "false")
    public Executor synchronousReminderExecutor() {
        return new SyncTaskExecutor();
    }
}
