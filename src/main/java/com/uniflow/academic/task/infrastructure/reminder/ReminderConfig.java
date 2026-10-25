package com.uniflow.academic.task.infrastructure.reminder;

import com.uniflow.academic.task.application.ports.out.ReminderScheduler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ReminderConfig {

    @Bean
    @ConditionalOnProperty(name = "reminder.scheduler", havingValue = "noop", matchIfMissing = true)
    public ReminderScheduler noopReminderScheduler() {
        return new NoopReminderScheduler();
    }
}
