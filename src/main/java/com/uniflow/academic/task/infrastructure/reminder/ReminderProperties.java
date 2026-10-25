package com.uniflow.academic.task.infrastructure.reminder;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

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
        @DefaultValue("24") int leadTimeHours
) {

    public enum Scheduler {
        NOOP,
        EVENTBRIDGE
    }

    public ReminderProperties {
        if (leadTimeHours < 0) {
            throw new IllegalArgumentException("reminder.lead-time-hours must be zero or positive");
        }
        if (scheduler == Scheduler.EVENTBRIDGE && (isBlank(queueArn) || isBlank(roleArn))) {
            throw new IllegalArgumentException(
                    "REMINDER_QUEUE_ARN and REMINDER_ROLE_ARN are required when REMINDER_SCHEDULER=eventbridge");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
