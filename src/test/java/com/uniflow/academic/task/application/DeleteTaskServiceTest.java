package com.uniflow.academic.task.application;

import com.uniflow.academic.task.application.services.DeleteTaskService;
import com.uniflow.academic.task.domain.TaskStatus;
import com.uniflow.academic.task.domain.exception.TaskNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static com.uniflow.academic.task.TaskFixtures.aTask;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeleteTaskServiceTest extends TaskServiceTestBase {

    private final DeleteTaskService service = new DeleteTaskService(repository, reminders);

    @Test
    void deleteRemovesTheTaskAndCancelsTheReminder() {
        service.execute("t1", USER);

        assertThat(stored("t1")).isNull();
        assertThat(scheduler.cancelled).containsExactly("t1");
    }

    @Test
    void deleteSurvivesASchedulerFailure() {
        scheduler.failing = true;

        service.execute("t1", USER);

        assertThat(stored("t1")).isNull();
    }

    @ParameterizedTest
    @EnumSource(TaskStatus.class)
    void aTaskInAnyStatusCanBeDeletedAndItsReminderIsCancelled(TaskStatus status) {
        repository.save(aTask().status(status).build());

        service.execute("t1", USER);

        assertThat(stored("t1")).isNull();
        assertThat(scheduler.cancelled).containsExactly("t1");
    }

    @Test
    void deleteOfAnotherUsersTaskIsNotFound() {
        assertThatThrownBy(() -> service.execute("t1", OTHER_USER)).isInstanceOf(TaskNotFoundException.class);
        assertThat(stored("t1")).isNotNull();
    }
}
