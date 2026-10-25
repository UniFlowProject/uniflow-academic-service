package com.uniflow.academic.task.infrastructure.reminder;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.context.properties.bind.BindException;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReminderPropertiesTest {

    private static ReminderProperties bind(Map<String, String> properties) {
        return new Binder(new MapConfigurationPropertySource(new HashMap<>(properties)))
                .bindOrCreate("reminder", ReminderProperties.class);
    }

    @Test
    void defaultsToTheNoopSchedulerWithADayOfLeadTime() {
        ReminderProperties properties = bind(Map.of());

        assertThat(properties.scheduler()).isEqualTo(ReminderProperties.Scheduler.NOOP);
        assertThat(properties.groupName()).isEqualTo("default");
        assertThat(properties.queueArn()).isNull();
        assertThat(properties.roleArn()).isNull();
        assertThat(properties.leadTimeHours()).isEqualTo(24);
        assertThat(properties.apiTimeout()).isEqualTo(Duration.ofSeconds(5));
        assertThat(properties.apiAttemptTimeout()).isEqualTo(Duration.ofSeconds(2));
        assertThat(properties.maxRetries()).isEqualTo(2);
        assertThat(properties.connectTimeout()).isEqualTo(Duration.ofSeconds(1));
        assertThat(properties.socketTimeout()).isEqualTo(Duration.ofSeconds(2));
    }

    @Test
    void readsTheClientTimeoutAndRetrySettings() {
        ReminderProperties properties = bind(Map.of(
                "reminder.api-timeout", "10s",
                "reminder.api-attempt-timeout", "3s",
                "reminder.max-retries", "0",
                "reminder.connect-timeout", "500ms",
                "reminder.socket-timeout", "4s"));

        assertThat(properties.apiTimeout()).isEqualTo(Duration.ofSeconds(10));
        assertThat(properties.apiAttemptTimeout()).isEqualTo(Duration.ofSeconds(3));
        assertThat(properties.maxRetries()).isZero();
        assertThat(properties.connectTimeout()).isEqualTo(Duration.ofMillis(500));
        assertThat(properties.socketTimeout()).isEqualTo(Duration.ofSeconds(4));
    }

    @ParameterizedTest
    @ValueSource(strings = {"reminder.api-timeout=0s", "reminder.api-attempt-timeout=-1s",
            "reminder.connect-timeout=0s", "reminder.socket-timeout=0s", "reminder.max-retries=-1"})
    void timeoutsMustBePositiveAndRetriesCannotBeNegative(String setting) {
        String[] parts = setting.split("=");

        assertThatThrownBy(() -> bind(Map.of(parts[0], parts[1]))).isInstanceOf(BindException.class);
    }

    @Test
    void readsEveryReminderSetting() {
        ReminderProperties properties = bind(Map.of(
                "reminder.scheduler", "eventbridge",
                "reminder.group-name", "uniflow",
                "reminder.queue-arn", "arn:queue",
                "reminder.role-arn", "arn:role",
                "reminder.lead-time-hours", "12"));

        assertThat(properties.scheduler()).isEqualTo(ReminderProperties.Scheduler.EVENTBRIDGE);
        assertThat(properties.groupName()).isEqualTo("uniflow");
        assertThat(properties.queueArn()).isEqualTo("arn:queue");
        assertThat(properties.roleArn()).isEqualTo("arn:role");
        assertThat(properties.leadTimeHours()).isEqualTo(12);
    }

    @Test
    void eventbridgeRequiresBothArns() {
        Map<String, String> noArns = Map.of("reminder.scheduler", "eventbridge");
        Map<String, String> onlyQueue = Map.of("reminder.scheduler", "eventbridge", "reminder.queue-arn", "arn:queue");
        Map<String, String> onlyRole = Map.of("reminder.scheduler", "eventbridge", "reminder.role-arn", "arn:role");
        Map<String, String> blankRole = Map.of("reminder.scheduler", "eventbridge",
                "reminder.queue-arn", "arn:queue", "reminder.role-arn", "  ");

        for (Map<String, String> properties : new Map[]{noArns, onlyQueue, onlyRole, blankRole}) {
            assertThatThrownBy(() -> bind(properties)).isInstanceOf(BindException.class);
        }
    }

    @Test
    void noopDoesNotRequireArns() {
        assertThat(bind(Map.of("reminder.scheduler", "noop", "reminder.queue-arn", "")).scheduler())
                .isEqualTo(ReminderProperties.Scheduler.NOOP);
    }

    @ParameterizedTest
    @ValueSource(strings = {"-1", "-24"})
    void leadTimeCannotBeNegative(String hours) {
        assertThatThrownBy(() -> bind(Map.of("reminder.lead-time-hours", hours))).isInstanceOf(BindException.class);
    }

    @Test
    void zeroLeadTimeIsAllowed() {
        assertThat(bind(Map.of("reminder.lead-time-hours", "0")).leadTimeHours()).isZero();
    }

    @Test
    void unknownSchedulerKindIsRejected() {
        assertThatThrownBy(() -> bind(Map.of("reminder.scheduler", "sns"))).isInstanceOf(BindException.class);
    }
}
