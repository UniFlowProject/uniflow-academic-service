package com.uniflow.academic.task.infrastructure.reminder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.scheduler.SchedulerClient;
import software.amazon.awssdk.services.scheduler.model.ActionAfterCompletion;
import software.amazon.awssdk.services.scheduler.model.ConflictException;
import software.amazon.awssdk.services.scheduler.model.CreateScheduleRequest;
import software.amazon.awssdk.services.scheduler.model.DeleteScheduleRequest;
import software.amazon.awssdk.services.scheduler.model.FlexibleTimeWindowMode;
import software.amazon.awssdk.services.scheduler.model.InternalServerException;
import software.amazon.awssdk.services.scheduler.model.ResourceNotFoundException;
import software.amazon.awssdk.services.scheduler.model.SchedulerException;
import software.amazon.awssdk.services.scheduler.model.UpdateScheduleRequest;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static com.uniflow.academic.task.TaskFixtures.NOW;
import static com.uniflow.academic.task.TaskFixtures.aTask;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EventBridgeReminderSchedulerTest {

    private static final String GROUP = "reminders";
    private static final String QUEUE_ARN = "arn:aws:sqs:us-east-1:123456789012:reminders";
    private static final String ROLE_ARN = "arn:aws:iam::123456789012:role/scheduler";

    private final SchedulerClient client = mock(SchedulerClient.class);
    private final ObjectMapper json = new ObjectMapper();
    private EventBridgeReminderScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = schedulerWithLeadTime(Duration.ofHours(24));
    }

    private EventBridgeReminderScheduler schedulerWithLeadTime(Duration leadTime) {
        return new EventBridgeReminderScheduler(
                client, Clock.fixed(NOW, ZoneOffset.UTC), GROUP, QUEUE_ARN, ROLE_ARN, leadTime);
    }

    private CreateScheduleRequest created() {
        ArgumentCaptor<CreateScheduleRequest> captor = ArgumentCaptor.forClass(CreateScheduleRequest.class);
        verify(client).createSchedule(captor.capture());
        return captor.getValue();
    }

    @Test
    void scheduleCreatesAOneTimeScheduleBeforeTheDueDate() {
        scheduler.schedule(aTask().id("t1").dueDate(NOW.plus(Duration.ofDays(3))).build());

        CreateScheduleRequest request = created();
        assertThat(request.name()).isEqualTo("task-reminder-t1");
        assertThat(request.groupName()).isEqualTo(GROUP);
        assertThat(request.scheduleExpression()).isEqualTo("at(2026-10-25T12:00:00)");
        assertThat(request.scheduleExpressionTimezone()).isEqualTo("UTC");
        assertThat(request.flexibleTimeWindow().mode()).isEqualTo(FlexibleTimeWindowMode.OFF);
        assertThat(request.actionAfterCompletion()).isEqualTo(ActionAfterCompletion.DELETE);
    }

    @Test
    void scheduleTargetsTheQueueWithThePayloadTheConsumerRequires() throws Exception {
        Instant due = NOW.plus(Duration.ofDays(3));

        scheduler.schedule(aTask().id("t1").userId("u1").title("Homework").dueDate(due).build());

        CreateScheduleRequest request = created();
        assertThat(request.target().arn()).isEqualTo(QUEUE_ARN);
        assertThat(request.target().roleArn()).isEqualTo(ROLE_ARN);
        JsonNode payload = json.readTree(request.target().input());
        List<String> keys = new ArrayList<>();
        payload.fieldNames().forEachRemaining(keys::add);
        assertThat(keys).containsExactlyInAnyOrder("type", "taskId", "userId", "taskTitle", "deadline");
        assertThat(payload.get("type").asText()).isEqualTo("task-reminder");
        assertThat(payload.get("taskId").asText()).isEqualTo("t1");
        assertThat(payload.get("userId").asText()).isEqualTo("u1");
        assertThat(payload.get("taskTitle").asText()).isEqualTo("Homework");
        assertThat(payload.get("deadline").asText()).isEqualTo(due.toString());
    }

    /**
     * Mirrors lambdas/process_expiration/index.mjs: it JSON.parses the SQS body, requires truthy
     * type, taskId and userId (validate), forwards type and userId as SNS String message attributes,
     * and builds the text with taskTitle (falling back to taskId) and deadline (falling back to
     * "pronto"). deadline is only interpolated into the text, never parsed as a date, so ISO-8601 is
     * a formatting choice that must stay readable and unambiguous (UTC instant).
     */
    @Test
    void payloadMatchesWhatTheProcessExpirationLambdaConsumes() throws Exception {
        Instant due = Instant.parse("2026-10-26T12:00:00Z");

        scheduler.schedule(aTask().id("t1").userId("u1").title("Homework").dueDate(due).build());

        JsonNode payload = json.readTree(created().target().input());
        assertThat(payload.isObject()).isTrue();
        List<String> keys = new ArrayList<>();
        payload.fieldNames().forEachRemaining(keys::add);
        assertThat(keys).containsExactlyInAnyOrder("type", "taskId", "userId", "taskTitle", "deadline");
        payload.fields().forEachRemaining(field ->
                assertThat(field.getValue().isTextual()).as(field.getKey() + " is a JSON string").isTrue());
        for (String required : List.of("type", "taskId", "userId")) {
            assertThat(payload.get(required).asText()).as("validate() needs truthy " + required).isNotEmpty();
        }
        assertThat(payload.get("deadline").asText()).matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?Z");
        assertThat(Instant.parse(payload.get("deadline").asText())).isEqualTo(due);
        String text = "Tu tarea \"%s\" vence el %s.".formatted(
                payload.get("taskTitle").asText(), payload.get("deadline").asText());
        assertThat(text).isEqualTo("Tu tarea \"Homework\" vence el 2026-10-26T12:00:00Z.");
    }

    @Test
    void payloadFillsEveryFieldTheConsumerLambdaValidates() throws Exception {
        scheduler.schedule(aTask().build());

        JsonNode payload = json.readTree(created().target().input());
        for (String required : List.of("type", "taskId", "userId")) {
            assertThat(payload.path(required).asText()).as(required).isNotBlank();
        }
    }

    @Test
    void fireTimeIsExpressedInUtcAndTruncatedToSeconds() {
        EventBridgeReminderScheduler twoHours = schedulerWithLeadTime(Duration.ofHours(2));

        twoHours.schedule(aTask().dueDate(Instant.parse("2026-10-30T10:30:15.000999Z")).build());

        assertThat(created().scheduleExpression()).isEqualTo("at(2026-10-30T08:30:15)");
    }

    @Test
    void scheduleNamesStayWithinTheServiceLimits() {
        scheduler.schedule(aTask().id("3f2b8c1e-9a4d-4e6b-8c7a-1d2e3f4a5b6c").build());

        String name = created().name();
        assertThat(name).hasSizeLessThanOrEqualTo(64).matches("[0-9a-zA-Z\\-_.]+");
    }

    @Test
    void scheduleReplacesAnExistingReminderOnConflict() {
        when(client.createSchedule(any(CreateScheduleRequest.class)))
                .thenThrow(ConflictException.builder().message("exists").build());

        scheduler.schedule(aTask().id("t1").title("New").dueDate(NOW.plus(Duration.ofDays(5))).build());

        ArgumentCaptor<UpdateScheduleRequest> captor = ArgumentCaptor.forClass(UpdateScheduleRequest.class);
        verify(client).updateSchedule(captor.capture());
        UpdateScheduleRequest update = captor.getValue();
        assertThat(update.name()).isEqualTo("task-reminder-t1");
        assertThat(update.groupName()).isEqualTo(GROUP);
        assertThat(update.scheduleExpression()).isEqualTo("at(2026-10-27T12:00:00)");
        assertThat(update.scheduleExpressionTimezone()).isEqualTo("UTC");
        assertThat(update.flexibleTimeWindow().mode()).isEqualTo(FlexibleTimeWindowMode.OFF);
        assertThat(update.actionAfterCompletion()).isEqualTo(ActionAfterCompletion.DELETE);
        assertThat(update.target().arn()).isEqualTo(QUEUE_ARN);
        assertThat(update.target().roleArn()).isEqualTo(ROLE_ARN);
        assertThat(update.target().input()).contains("\"taskTitle\":\"New\"");
    }

    @ParameterizedTest
    @ValueSource(longs = {24, 10, 0, -5})
    void scheduleSkipsWithoutCallingAwsWhenTheFireTimeIsNotInTheFuture(long hoursUntilDue) {
        scheduler.schedule(aTask().id("t1").dueDate(NOW.plus(Duration.ofHours(hoursUntilDue))).build());

        verify(client, never()).createSchedule(any(CreateScheduleRequest.class));
        verify(client, never()).updateSchedule(any(UpdateScheduleRequest.class));
        verify(client, never()).deleteSchedule(any(DeleteScheduleRequest.class));
    }

    @ParameterizedTest
    @ValueSource(longs = {24, 10, 0, -5})
    void rescheduleRemovesTheStaleReminderWhenTheFireTimeIsNotInTheFuture(long hoursUntilDue) {
        scheduler.reschedule(aTask().id("t1").dueDate(NOW.plus(Duration.ofHours(hoursUntilDue))).build());

        verify(client, never()).createSchedule(any(CreateScheduleRequest.class));
        ArgumentCaptor<DeleteScheduleRequest> captor = ArgumentCaptor.forClass(DeleteScheduleRequest.class);
        verify(client).deleteSchedule(captor.capture());
        assertThat(captor.getValue().name()).isEqualTo("task-reminder-t1");
        assertThat(captor.getValue().groupName()).isEqualTo(GROUP);
    }

    @Test
    void rescheduleReplacesTheScheduleWhenTheFireTimeIsInTheFuture() {
        when(client.createSchedule(any(CreateScheduleRequest.class)))
                .thenThrow(ConflictException.builder().message("exists").build());

        scheduler.reschedule(aTask().id("t1").title("Renamed").dueDate(NOW.plus(Duration.ofDays(5))).build());

        ArgumentCaptor<UpdateScheduleRequest> captor = ArgumentCaptor.forClass(UpdateScheduleRequest.class);
        verify(client).updateSchedule(captor.capture());
        assertThat(captor.getValue().target().input()).contains("\"taskTitle\":\"Renamed\"");
        verify(client, never()).deleteSchedule(any(DeleteScheduleRequest.class));
    }

    @Test
    void zeroLeadTimeSchedulesAtTheDueDate() {
        schedulerWithLeadTime(Duration.ZERO).schedule(aTask().dueDate(NOW.plus(Duration.ofHours(5))).build());

        assertThat(created().scheduleExpression()).isEqualTo("at(2026-10-23T17:00:00)");
    }

    @Test
    void cancelDeletesTheSchedule() {
        scheduler.cancel("t1");

        ArgumentCaptor<DeleteScheduleRequest> captor = ArgumentCaptor.forClass(DeleteScheduleRequest.class);
        verify(client).deleteSchedule(captor.capture());
        assertThat(captor.getValue().name()).isEqualTo("task-reminder-t1");
        assertThat(captor.getValue().groupName()).isEqualTo(GROUP);
    }

    @Test
    void cancelOfAMissingScheduleIsANoop() {
        when(client.deleteSchedule(any(DeleteScheduleRequest.class)))
                .thenThrow(ResourceNotFoundException.builder().message("missing").build());

        scheduler.cancel("missing");
    }

    @Test
    void otherClientErrorsPropagateOnSchedule() {
        when(client.createSchedule(any(CreateScheduleRequest.class)))
                .thenThrow(SchedulerException.builder().message("denied").build());

        assertThatThrownBy(() -> scheduler.schedule(aTask().build())).isInstanceOf(SchedulerException.class);
    }

    @Test
    void failuresWhileReplacingPropagate() {
        when(client.createSchedule(any(CreateScheduleRequest.class)))
                .thenThrow(ConflictException.builder().message("exists").build());
        when(client.updateSchedule(any(UpdateScheduleRequest.class)))
                .thenThrow(InternalServerException.builder().message("down").build());

        assertThatThrownBy(() -> scheduler.schedule(aTask().build())).isInstanceOf(InternalServerException.class);
    }

    @Test
    void otherClientErrorsPropagateOnCancel() {
        when(client.deleteSchedule(any(DeleteScheduleRequest.class)))
                .thenThrow(InternalServerException.builder().message("down").build());

        assertThatThrownBy(() -> scheduler.cancel("t1")).isInstanceOf(InternalServerException.class);
    }
}
