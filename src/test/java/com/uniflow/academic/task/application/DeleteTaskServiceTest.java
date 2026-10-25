package com.uniflow.academic.task.application;

import com.uniflow.academic.task.application.services.DeleteTaskService;
import com.uniflow.academic.task.domain.TaskStatus;
import com.uniflow.academic.task.domain.exception.TaskNotFoundException;
import com.uniflow.academic.task.domain.exception.TaskNotModifiableException;
import org.junit.jupiter.api.Test;

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

    @Test
    void deletingAFinishedTaskIsRejected() {
        repository.save(aTask().status(TaskStatus.CANCELLED).build());

        assertThatThrownBy(() -> service.execute("t1", USER)).isInstanceOf(TaskNotModifiableException.class);
        assertThat(stored("t1")).isNotNull();
        assertThat(scheduler.cancelled).isEmpty();
    }

    @Test
    void deleteOfAnotherUsersTaskIsNotFound() {
        assertThatThrownBy(() -> service.execute("t1", OTHER_USER)).isInstanceOf(TaskNotFoundException.class);
        assertThat(stored("t1")).isNotNull();
    }
}
