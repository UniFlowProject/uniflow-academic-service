package com.uniflow.academic.task.infrastructure.reminder;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Reminder settings ({@code REMINDER_*} environment variables). The EventBridge scheduler needs
 * both ARNs, so a misconfigured deployment fails at startup instead of at the first task.
 */
@ConfigurationProperties(prefix = "reminder")
public record ReminderProperties(
        @DefaultValue("noop") Scheduler scheduler,
        @DefaultValue("default") String groupName,
        String queueArn,
        String roleArn,
        @DefaultValue("24") int leadTimeHours,
        @DefaultValue("5s") Duration apiTimeout,
        @DefaultValue("2s") Duration apiAttemptTimeout,
        @DefaultValue("2") int maxRetries,
        @DefaultValue("1s") Duration connectTimeout,
        @DefaultValue("2s") Duration socketTimeout
) {

    public enum Scheduler {
        NOOP,
        EVENTBRIDGE
    }

    public ReminderProperties {
        if (leadTimeHours < 0) {
            throw new IllegalArgumentException("reminder.lead-time-hours must be zero or positive");
        }
        if (maxRetries < 0) {
            throw new IllegalArgumentException("reminder.max-retries must be zero or positive");
        }
        requirePositive("reminder.api-timeout", apiTimeout);
        requirePositive("reminder.api-attempt-timeout", apiAttemptTimeout);
        requirePositive("reminder.connect-timeout", connectTimeout);
        requirePositive("reminder.socket-timeout", socketTimeout);
        if (scheduler == Scheduler.EVENTBRIDGE && (isBlank(queueArn) || isBlank(roleArn))) {
            throw new IllegalArgumentException(
                    "REMINDER_QUEUE_ARN and REMINDER_ROLE_ARN are required when REMINDER_SCHEDULER=eventbridge");
        }
    }

    private static void requirePositive(String name, Duration value) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(name + " must be positive");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
