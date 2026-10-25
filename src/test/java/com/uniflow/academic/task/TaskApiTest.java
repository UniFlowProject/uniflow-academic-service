package com.uniflow.academic.task;

import com.uniflow.academic.support.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TaskApiTest extends IntegrationTestBase {

    private String user;
    private String periodId;
    private String courseId;

    @BeforeEach
    void setUp() throws Exception {
        user = newUserId();
        periodId = createPeriod(user, "P1");
        courseId = createCourse(user, periodId, "MAT-101");
    }

    private String taskJson(String extra) {
        return """
                {"title":"Essay","courseId":"%s","periodId":"%s","dueDate":"2099-11-01T10:00:00Z",
                 "priority":"high","type":"essay","estimatedTimeHours":4%s}
                """.formatted(courseId, periodId, extra.isEmpty() ? "" : "," + extra);
    }

    private String createTask(String extra) throws Exception {
        MvcResult result = mockMvc.perform(postAs(user, "/tasks", taskJson(extra)))
                .andExpect(status().isCreated())
                .andReturn();
        return json(result.getResponse().getContentAsString()).get("id").asText();
    }

    private String createTask() throws Exception {
        return createTask("");
    }

    private void moveTo(String taskId, String status) throws Exception {
        mockMvc.perform(patchAs(user, "/tasks/" + taskId + "/status").contentType("application/json")
                        .content("{\"status\":\"%s\"}".formatted(status)))
                .andExpect(status().isOk());
    }

    private MockHttpServletRequestBuilder patchJson(String path, String body) {
        return patchAs(user, path).contentType("application/json").content(body);
    }

    // --- create / read -------------------------------------------------------------------

    @Test
    void createReturns201WithACamelCaseBody() throws Exception {
        mockMvc.perform(postAs(user, "/tasks", taskJson("")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.userId").value(user))
                .andExpect(jsonPath("$.title").value("Essay"))
                .andExpect(jsonPath("$.description").value(""))
                .andExpect(jsonPath("$.courseId").value(courseId))
                .andExpect(jsonPath("$.periodId").value(periodId))
                .andExpect(jsonPath("$.dueDate").value("2099-11-01T10:00:00Z"))
                .andExpect(jsonPath("$.status").value("todo"))
                .andExpect(jsonPath("$.priority").value("high"))
                .andExpect(jsonPath("$.type").value("essay"))
                .andExpect(jsonPath("$.estimatedTimeHours").value(4))
                .andExpect(jsonPath("$.actualTimeHours").value(nullValue()))
                .andExpect(jsonPath("$.tags", hasSize(0)))
                .andExpect(jsonPath("$.groupMembers", hasSize(0)))
                .andExpect(jsonPath("$.attachments", hasSize(0)))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty())
                .andExpect(jsonPath("$.completedAt").value(nullValue()));
    }

    @Test
    void createSchedulesAReminderAfterCommit() throws Exception {
        String taskId = createTask();

        assertThat(reminders.scheduled()).containsExactly(taskId);
    }

    @Test
    void createSucceedsEvenIfTheReminderSchedulerFails() throws Exception {
        reminders.failing(true);

        String taskId = createTask();

        mockMvc.perform(getAs(user, "/tasks/" + taskId)).andExpect(status().isOk());
    }

    @Test
    void createAcceptsOptionalFieldsAndGroupMembersForGroupWork() throws Exception {
        String body = """
                {"title":"Project","description":"d","courseId":"%s","periodId":"%s",
                 "dueDate":"2099-11-01T10:00:00+02:00","priority":"low","type":"group-work",
                 "estimatedTimeHours":0,"tags":["a"],"groupMembers":["u2"],"attachments":["x.pdf"]}
                """.formatted(courseId, periodId);

        mockMvc.perform(postAs(user, "/tasks", body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.dueDate").value("2099-11-01T08:00:00Z"))
                .andExpect(jsonPath("$.groupMembers[0]").value("u2"))
                .andExpect(jsonPath("$.tags[0]").value("a"))
                .andExpect(jsonPath("$.attachments[0]").value("x.pdf"));
    }

    @Test
    void getReturnsTheTask() throws Exception {
        String taskId = createTask();

        mockMvc.perform(getAs(user, "/tasks/" + taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Essay"));
    }

    // --- list ----------------------------------------------------------------------------

    @Test
    void listReturnsItemsAndPageInfoWithDefaultLimitTwenty() throws Exception {
        createTask();
        String other = newUserId();
        mockMvc.perform(getAs(other, "/tasks")).andExpect(jsonPath("$.items", hasSize(0)));

        mockMvc.perform(getAs(user, "/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.pageInfo.total").value(1))
                .andExpect(jsonPath("$.pageInfo.page").value(1))
                .andExpect(jsonPath("$.pageInfo.limit").value(20))
                .andExpect(jsonPath("$.pageInfo.totalPages").value(1))
                .andExpect(jsonPath("$.pageInfo.hasNext").value(false))
                .andExpect(jsonPath("$.pageInfo.hasPrev").value(false));
    }

    @Test
    void listMapsQueryParametersToTheFilter() throws Exception {
        String periodTwo = createPeriod(user, "P2");
        String otherCourse = createCourse(user, periodTwo, "FIS-101");
        String done = createTask();
        moveTo(done, "in-progress");
        moveTo(done, "done");
        String inProgress = createTask();
        moveTo(inProgress, "in-progress");
        createTask();
        mockMvc.perform(postAs(user, "/tasks", """
                {"title":"Other","courseId":"%s","periodId":"%s","dueDate":"2099-11-01T10:00:00Z",
                 "priority":"low","type":"exam","estimatedTimeHours":1}
                """.formatted(otherCourse, periodTwo))).andExpect(status().isCreated());

        mockMvc.perform(getAs(user, "/tasks")
                        .param("status", "todo", "in-progress")
                        .param("courseId", courseId)
                        .param("sortBy", "status").param("sortOrder", "desc")
                        .param("page", "1").param("limit", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].id").value(inProgress))
                .andExpect(jsonPath("$.pageInfo.total").value(2))
                .andExpect(jsonPath("$.pageInfo.hasNext").value(true));

        mockMvc.perform(getAs(user, "/tasks").param("periodId", periodTwo).param("priority", "low"))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].title").value("Other"));
    }

    @Test
    void listSupportsSearchDueDateRangeAndOverdueFlag() throws Exception {
        mockMvc.perform(postAs(user, "/tasks", taskJson("\"description\":\"Calculus chapter\"")))
                .andExpect(status().isCreated());
        mockMvc.perform(postAs(user, "/tasks", taskJson("\"description\":\"Other\"")))
                .andExpect(status().isCreated());

        mockMvc.perform(getAs(user, "/tasks")
                        .param("search", "CALC")
                        .param("dueDateFrom", "2099-10-31T00:00:00Z")
                        .param("dueDateTo", "2099-11-02T00:00:00Z")
                        .param("isOverdue", "false")
                        .param("isDueSoon", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].description").value("Calculus chapter"));
        mockMvc.perform(getAs(user, "/tasks").param("isOverdue", "true"))
                .andExpect(jsonPath("$.items", hasSize(0)));
    }

    @ParameterizedTest
    @CsvSource({
            "limit,1000", "limit,0", "page,0", "page,-1", "status,bogus", "priority,bogus",
            "sortBy,bogus", "sortOrder,sideways", "dueDateFrom,yesterday", "isOverdue,maybe", "limit,abc"
    })
    void listRejectsInvalidQueryParameters(String name, String value) throws Exception {
        mockMvc.perform(getAs(user, "/tasks").param(name, value))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void listIgnoresTheUnusedTimeZoneParameter() throws Exception {
        mockMvc.perform(getAs(user, "/tasks").param("timeZone", "Mars/Base")).andExpect(status().isOk());
    }

    // --- update --------------------------------------------------------------------------

    @Test
    void patchAppliesPartialChanges() throws Exception {
        String taskId = createTask();

        mockMvc.perform(patchJson("/tasks/" + taskId, "{\"title\":\"New title\",\"priority\":\"urgent\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("New title"))
                .andExpect(jsonPath("$.priority").value("urgent"))
                .andExpect(jsonPath("$.courseId").value(courseId))
                .andExpect(jsonPath("$.estimatedTimeHours").value(4));
        assertThat(reminders.scheduled()).containsExactly(taskId);
    }

    @Test
    void patchReschedulesTheReminderOnlyWhenTheDueDateChanges() throws Exception {
        String taskId = createTask();
        reminders.reset();

        mockMvc.perform(patchJson("/tasks/" + taskId, "{\"title\":\"x\"}")).andExpect(status().isOk());
        assertThat(reminders.scheduled()).isEmpty();

        mockMvc.perform(patchJson("/tasks/" + taskId, "{\"dueDate\":\"2099-12-01T10:00:00Z\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dueDate").value("2099-12-01T10:00:00Z"));
        assertThat(reminders.scheduled()).containsExactly(taskId);
    }

    @Test
    void patchWithAnEmptyBodyChangesNothing() throws Exception {
        String taskId = createTask();

        mockMvc.perform(patchJson("/tasks/" + taskId, "{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Essay"));
    }

    @Test
    void patchSetsAndExplicitlyClearsTheActualHours() throws Exception {
        String taskId = createTask();

        mockMvc.perform(patchJson("/tasks/" + taskId, "{\"actualTimeHours\":6}"))
                .andExpect(jsonPath("$.actualTimeHours").value(6));
        mockMvc.perform(patchJson("/tasks/" + taskId, "{\"title\":\"x\"}"))
                .andExpect(jsonPath("$.actualTimeHours").value(6));
        mockMvc.perform(patchJson("/tasks/" + taskId, "{\"actualTimeHours\":null}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actualTimeHours").value(nullValue()));
    }

    @ParameterizedTest
    @CsvSource({
            "title", "description", "courseId", "periodId", "dueDate", "priority", "type",
            "estimatedTimeHours", "tags", "groupMembers", "attachments"
    })
    void patchRejectsAnExplicitNullOnRequiredFields(String field) throws Exception {
        String taskId = createTask();

        mockMvc.perform(patchJson("/tasks/" + taskId, "{\"%s\":null}".formatted(field)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.message", containsString(field)));
    }

    @Test
    void patchRejectsInvalidValues() throws Exception {
        String taskId = createTask();

        mockMvc.perform(patchJson("/tasks/" + taskId, "{\"title\":\"\"}")).andExpect(status().isBadRequest());
        mockMvc.perform(patchJson("/tasks/" + taskId, "{\"title\":\"  \"}")).andExpect(status().isBadRequest());
        mockMvc.perform(patchJson("/tasks/" + taskId, "{\"estimatedTimeHours\":-1}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(patchJson("/tasks/" + taskId, "{\"actualTimeHours\":-1}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(patchJson("/tasks/" + taskId, "{\"priority\":\"bogus\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details[*].field", hasItem("priority")));
    }

    @Test
    void patchViolatingADomainInvariantIsInvalidTask() throws Exception {
        String taskId = createTask();

        mockMvc.perform(patchJson("/tasks/" + taskId, "{\"groupMembers\":[\"u2\"]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_TASK"));
    }

    @Test
    void patchRevalidatesTheCourseAndPeriod() throws Exception {
        String taskId = createTask();
        String periodTwo = createPeriod(user, "P2");
        String courseTwo = createCourse(user, periodTwo, "FIS-101");
        String foreignPeriod = createPeriod(newUserId(), "X");

        mockMvc.perform(patchJson("/tasks/" + taskId, "{\"courseId\":\"%s\"}".formatted(courseTwo)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_TASK"));
        mockMvc.perform(patchJson("/tasks/" + taskId, "{\"courseId\":\"nope\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("COURSE_NOT_FOUND"));
        mockMvc.perform(patchJson("/tasks/" + taskId, "{\"periodId\":\"%s\"}".formatted(foreignPeriod)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("PERIOD_NOT_FOUND"));
        mockMvc.perform(patchJson("/tasks/" + taskId,
                        "{\"periodId\":\"%s\",\"courseId\":\"%s\"}".formatted(periodTwo, courseTwo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courseId").value(courseTwo))
                .andExpect(jsonPath("$.periodId").value(periodTwo));
    }

    // --- status --------------------------------------------------------------------------

    @Test
    void changeStatusReturnsTheUpdatedTask() throws Exception {
        String taskId = createTask();

        mockMvc.perform(patchJson("/tasks/" + taskId + "/status", "{\"status\":\"in-progress\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("in-progress"));
        mockMvc.perform(getAs(user, "/tasks/" + taskId)).andExpect(jsonPath("$.status").value("in-progress"));
        assertThat(reminders.cancelled()).isEmpty();
    }

    @Test
    void completingATaskRecordsCompletionAndCancelsTheReminder() throws Exception {
        String taskId = createTask();
        moveTo(taskId, "in-progress");

        mockMvc.perform(patchJson("/tasks/" + taskId + "/status", "{\"status\":\"done\"}"))
                .andExpect(jsonPath("$.status").value("done"))
                .andExpect(jsonPath("$.completedAt").isNotEmpty());
        assertThat(reminders.cancelled()).containsExactly(taskId);
    }

    @Test
    void cancellingATaskCancelsTheReminder() throws Exception {
        String taskId = createTask();

        moveTo(taskId, "cancelled");

        assertThat(reminders.cancelled()).containsExactly(taskId);
    }

    @Test
    void invalidStatusTransitionIsAConflict() throws Exception {
        String taskId = createTask();

        mockMvc.perform(patchJson("/tasks/" + taskId + "/status", "{\"status\":\"done\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("INVALID_STATUS_TRANSITION"));
        mockMvc.perform(getAs(user, "/tasks/" + taskId)).andExpect(jsonPath("$.status").value("todo"));
        assertThat(reminders.cancelled()).isEmpty();
    }

    @Test
    void changeStatusRejectsMissingOrUnknownStatus() throws Exception {
        String taskId = createTask();

        mockMvc.perform(patchJson("/tasks/" + taskId + "/status", "{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        mockMvc.perform(patchJson("/tasks/" + taskId + "/status", "{\"status\":\"bogus\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    // --- delete --------------------------------------------------------------------------

    @Test
    void deleteReturns204AndCancelsTheReminder() throws Exception {
        String taskId = createTask();

        mockMvc.perform(deleteAs(user, "/tasks/" + taskId))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        mockMvc.perform(getAs(user, "/tasks/" + taskId)).andExpect(status().isNotFound());
        assertThat(reminders.cancelled()).containsExactly(taskId);
    }

    // --- stats ---------------------------------------------------------------------------

    @Test
    void statsEndpointIsNotShadowedByTheTaskIdRoute() throws Exception {
        String done = createTask();
        moveTo(done, "in-progress");
        moveTo(done, "done");
        createTask();

        mockMvc.perform(getAs(user, "/tasks/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.completed").value(1))
                .andExpect(jsonPath("$.pending").value(1))
                .andExpect(jsonPath("$.overdue").value(0))
                .andExpect(jsonPath("$.byStatus.done").value(1))
                .andExpect(jsonPath("$.byStatus.todo").value(1))
                .andExpect(jsonPath("$.byStatus['in-review']").doesNotExist())
                .andExpect(jsonPath("$.byPriority.high").value(2))
                .andExpect(jsonPath("$.byType.essay").value(2))
                .andExpect(jsonPath("$.byCourse['%s']".formatted(courseId)).value(2));
        mockMvc.perform(getAs(newUserId(), "/tasks/stats")).andExpect(jsonPath("$.total").value(0));
    }

    // --- errors --------------------------------------------------------------------------

    @Test
    void unknownTaskIsNotFound() throws Exception {
        mockMvc.perform(getAs(user, "/tasks/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("TASK_NOT_FOUND"))
                .andExpect(jsonPath("$.error.message", containsString("missing")));
    }

    @Test
    void anotherUsersTaskIsNotFoundOnEveryEndpoint() throws Exception {
        String taskId = createTask();
        String intruder = newUserId();

        mockMvc.perform(getAs(intruder, "/tasks/" + taskId))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error.code").value("TASK_NOT_FOUND"));
        mockMvc.perform(patchAs(intruder, "/tasks/" + taskId).contentType("application/json").content("{}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(patchAs(intruder, "/tasks/" + taskId + "/status").contentType("application/json")
                        .content("{\"status\":\"in-progress\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(deleteAs(intruder, "/tasks/" + taskId)).andExpect(status().isNotFound());
        mockMvc.perform(getAs(user, "/tasks/" + taskId)).andExpect(status().isOk());
    }

    @Test
    void modifyingAClosedTaskIsAConflict() throws Exception {
        String done = createTask();
        moveTo(done, "in-progress");
        moveTo(done, "done");
        String cancelled = createTask();
        moveTo(cancelled, "cancelled");

        for (String taskId : new String[]{done, cancelled}) {
            mockMvc.perform(patchJson("/tasks/" + taskId, "{\"title\":\"x\"}"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error.code").value("TASK_NOT_MODIFIABLE"));
            mockMvc.perform(patchJson("/tasks/" + taskId + "/status", "{\"status\":\"todo\"}"))
                    .andExpect(status().isConflict());
        }
    }

    @Test
    void closedTasksCanBeDeleted() throws Exception {
        String done = createTask();
        moveTo(done, "in-progress");
        moveTo(done, "done");
        String cancelled = createTask();
        moveTo(cancelled, "cancelled");
        reminders.reset();

        mockMvc.perform(deleteAs(user, "/tasks/" + done)).andExpect(status().isNoContent());
        mockMvc.perform(deleteAs(user, "/tasks/" + cancelled)).andExpect(status().isNoContent());

        mockMvc.perform(getAs(user, "/tasks/" + done)).andExpect(status().isNotFound());
        assertThat(reminders.cancelled()).containsExactly(done, cancelled);
    }

    @Test
    void invalidRequestBodyIsAValidationErrorWithDetails() throws Exception {
        String body = taskJson("").replace("\"title\":\"Essay\",", "");

        mockMvc.perform(postAs(user, "/tasks", body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details[*].field", hasItem("title")));
    }

    @Test
    void blankTitleNegativeHoursAndBadEnumsAreRejected() throws Exception {
        mockMvc.perform(postAs(user, "/tasks", taskJson("").replace("\"Essay\"", "\"\"")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(postAs(user, "/tasks", taskJson("").replace("\"estimatedTimeHours\":4", "\"estimatedTimeHours\":-1")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(postAs(user, "/tasks", taskJson("").replace("\"high\"", "\"critical\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details[*].field", hasItem("priority")));
        mockMvc.perform(postAs(user, "/tasks", taskJson("").replace("\"essay\"", "\"homework\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details[*].field", hasItem("type")));
        mockMvc.perform(postAs(user, "/tasks", taskJson("").replace("\"estimatedTimeHours\":4", "\"x\":1")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(postAs(user, "/tasks", "{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("MALFORMED_REQUEST"));
        mockMvc.perform(getAs(user, "/tasks")).andExpect(jsonPath("$.items", hasSize(0)));
    }

    @Test
    void groupMembersOnANonGroupTaskIsInvalidTask() throws Exception {
        mockMvc.perform(postAs(user, "/tasks", taskJson("\"groupMembers\":[\"u2\"]")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_TASK"));
        assertThat(reminders.scheduled()).isEmpty();
    }

    @Test
    void createInAForeignOrUnknownPeriodOrCourseIsNotFound() throws Exception {
        String other = newUserId();
        String otherPeriod = createPeriod(other, "P");
        String otherCourse = createCourse(other, otherPeriod, "MAT-101");

        mockMvc.perform(postAs(user, "/tasks", taskJson("").replace(periodId, otherPeriod)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("PERIOD_NOT_FOUND"));
        mockMvc.perform(postAs(user, "/tasks", taskJson("").replace(courseId, otherCourse)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("COURSE_NOT_FOUND"));
        mockMvc.perform(postAs(user, "/tasks", taskJson("").replace(courseId, "nope")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("COURSE_NOT_FOUND"));
        mockMvc.perform(getAs(user, "/tasks")).andExpect(jsonPath("$.items", hasSize(0)));
    }

    @Test
    void createWithACourseOutsideThePeriodIsInvalidTask() throws Exception {
        String periodTwo = createPeriod(user, "P2");

        mockMvc.perform(postAs(user, "/tasks", taskJson("").replace(periodId, periodTwo)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_TASK"));
    }

    // --- security ------------------------------------------------------------------------

    @ParameterizedTest
    @CsvSource({
            "POST,/tasks", "GET,/tasks", "GET,/tasks/stats", "GET,/tasks/t1",
            "PATCH,/tasks/t1", "PATCH,/tasks/t1/status", "DELETE,/tasks/t1"
    })
    void everyEndpointRequiresAuthentication(String method, String path) throws Exception {
        MockHttpServletRequestBuilder request = switch (method) {
            case "POST" -> post(path);
            case "PATCH" -> patch(path);
            case "DELETE" -> delete(path);
            default -> get(path);
        };

        mockMvc.perform(request.contentType("application/json").content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void tasksAreOwnedByTheJwtSubject() throws Exception {
        String taskId = createTask();

        mockMvc.perform(getAs(user, "/tasks")).andExpect(jsonPath("$.items[*].id", hasItem(taskId)));
        mockMvc.perform(getAs(newUserId(), "/tasks")).andExpect(jsonPath("$.items[*].id", not(hasItem(taskId))));
    }
}
