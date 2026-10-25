package com.uniflow.academic.task.infrastructure.reminder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uniflow.academic.task.application.ports.out.ReminderScheduler;
import com.uniflow.academic.task.domain.Task;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.scheduler.SchedulerClient;
import software.amazon.awssdk.services.scheduler.model.ActionAfterCompletion;
import software.amazon.awssdk.services.scheduler.model.ConflictException;
import software.amazon.awssdk.services.scheduler.model.CreateScheduleRequest;
import software.amazon.awssdk.services.scheduler.model.DeleteScheduleRequest;
import software.amazon.awssdk.services.scheduler.model.FlexibleTimeWindowMode;
import software.amazon.awssdk.services.scheduler.model.ResourceNotFoundException;
import software.amazon.awssdk.services.scheduler.model.UpdateScheduleRequest;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Each reminder is a one-time EventBridge schedule that delivers a small JSON message to an SQS
 * queue and deletes itself once it has fired. The message follows the contract of the
 * {@code process_expiration} consumer: {@code type}, {@code taskId} and {@code userId} are required,
 * {@code taskTitle} and {@code deadline} are used to build the notification text.
 */
@Slf4j
public class EventBridgeReminderScheduler implements ReminderScheduler {

    static final String REMINDER_TYPE = "task-reminder";

    private static final String NAME_PREFIX = "task-reminder-";
    private static final String TIMEZONE = "UTC";
    private static final DateTimeFormatter AT_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss").withZone(ZoneOffset.UTC);

    private final SchedulerClient client;
    private final Clock clock;
    private final String groupName;
    private final String queueArn;
    private final String roleArn;
    private final Duration leadTime;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public EventBridgeReminderScheduler(
            SchedulerClient client, Clock clock, String groupName, String queueArn, String roleArn, Duration leadTime
    ) {
        this.client = client;
        this.clock = clock;
        this.groupName = groupName;
        this.queueArn = queueArn;
        this.roleArn = roleArn;
        this.leadTime = leadTime;
    }

    @Override
    public void schedule(Task task) {
        Instant fireAt = task.getDueDate().minus(leadTime);
        if (!fireAt.isAfter(clock.instant())) {
            cancel(task.getId());
            return;
        }
        String name = scheduleName(task.getId());
        String expression = "at(" + AT_FORMAT.format(fireAt) + ")";
        String payload = payload(task);
        try {
            client.createSchedule(CreateScheduleRequest.builder()
                    .name(name)
                    .groupName(groupName)
                    .scheduleExpression(expression)
                    .scheduleExpressionTimezone(TIMEZONE)
                    .flexibleTimeWindow(window -> window.mode(FlexibleTimeWindowMode.OFF))
                    .target(target -> target.arn(queueArn).roleArn(roleArn).input(payload))
                    .actionAfterCompletion(ActionAfterCompletion.DELETE)
                    .build());
        } catch (ConflictException alreadyScheduled) {
            client.updateSchedule(UpdateScheduleRequest.builder()
                    .name(name)
                    .groupName(groupName)
                    .scheduleExpression(expression)
                    .scheduleExpressionTimezone(TIMEZONE)
                    .flexibleTimeWindow(window -> window.mode(FlexibleTimeWindowMode.OFF))
                    .target(target -> target.arn(queueArn).roleArn(roleArn).input(payload))
                    .actionAfterCompletion(ActionAfterCompletion.DELETE)
                    .build());
        }
    }

    @Override
    public void cancel(String taskId) {
        try {
            client.deleteSchedule(DeleteScheduleRequest.builder()
                    .name(scheduleName(taskId))
                    .groupName(groupName)
                    .build());
        } catch (ResourceNotFoundException alreadyGone) {
            log.debug("No reminder to cancel for task {}", taskId);
        }
    }

    private static String scheduleName(String taskId) {
        return NAME_PREFIX + taskId;
    }

    private String payload(Task task) {
        Map<String, String> message = new LinkedHashMap<>();
        message.put("type", REMINDER_TYPE);
        message.put("taskId", task.getId());
        message.put("userId", task.getUserId());
        message.put("taskTitle", task.getTitle());
        message.put("deadline", task.getDueDate().toString());
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to serialize the reminder payload", e);
        }
    }
}
