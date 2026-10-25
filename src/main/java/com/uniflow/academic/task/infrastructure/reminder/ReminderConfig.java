package com.uniflow.academic.task.infrastructure.reminder;

import com.uniflow.academic.task.application.ports.out.ReminderScheduler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.scheduler.SchedulerClient;

import java.time.Clock;
import java.time.Duration;

@Configuration
@EnableConfigurationProperties(ReminderProperties.class)
public class ReminderConfig {

    @Bean
    @ConditionalOnProperty(name = "reminder.scheduler", havingValue = "noop", matchIfMissing = true)
    public ReminderScheduler noopReminderScheduler() {
        return new NoopReminderScheduler();
    }

    /** Region and credentials come from the AWS default provider chains (task role on ECS). */
    @Bean
    @ConditionalOnProperty(name = "reminder.scheduler", havingValue = "eventbridge")
    @ConditionalOnMissingBean(SchedulerClient.class)
    public SchedulerClient schedulerClient() {
        return SchedulerClient.create();
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
}
