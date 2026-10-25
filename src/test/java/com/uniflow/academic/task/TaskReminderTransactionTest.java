package com.uniflow.academic.task;

import com.uniflow.academic.support.IntegrationTestBase;
import com.uniflow.academic.task.application.ports.in.CreateTaskCommand;
import com.uniflow.academic.task.application.ports.in.DeleteTaskCommand;
import com.uniflow.academic.task.domain.Task;
import com.uniflow.academic.task.domain.TaskDraft;
import com.uniflow.academic.task.domain.TaskPriority;
import com.uniflow.academic.task.domain.TaskType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TaskReminderTransactionTest extends IntegrationTestBase {

    @Autowired
    private CreateTaskCommand createTask;

    @Autowired
    private DeleteTaskCommand deleteTask;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private String user;
    private TaskDraft draft;

    @BeforeEach
    void setUp() throws Exception {
        user = newUserId();
        String periodId = createPeriod(user, "P1");
        String courseId = createCourse(user, periodId, "MAT-101");
        draft = new TaskDraft("Essay", null, courseId, periodId, Instant.parse("2099-01-01T00:00:00Z"),
                TaskPriority.HIGH, TaskType.ESSAY, 1, null, null, null);
    }

    @Test
    void reminderIsScheduledOnlyAfterTheOuterTransactionCommits() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        Task task = tx.execute(status -> {
            Task created = createTask.execute(draft, user);
            assertThat(reminders.scheduled()).isEmpty();
            return created;
        });

        assertThat(reminders.scheduled()).containsExactly(task.getId());
    }

    @Test
    void rolledBackCreateLeavesNoReminderAndNoTask() throws Exception {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        assertThatThrownBy(() -> tx.executeWithoutResult(status -> {
            createTask.execute(draft, user);
            throw new IllegalStateException("boom");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(reminders.scheduled()).isEmpty();
        mockMvc.perform(getAs(user, "/tasks"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.items")
                        .isEmpty());
    }

    @Test
    void rolledBackDeleteDoesNotCancelTheReminder() {
        Task task = createTask.execute(draft, user);
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        assertThatThrownBy(() -> tx.executeWithoutResult(status -> {
            deleteTask.execute(task.getId(), user);
            throw new IllegalStateException("boom");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(reminders.cancelled()).isEmpty();
    }
}
