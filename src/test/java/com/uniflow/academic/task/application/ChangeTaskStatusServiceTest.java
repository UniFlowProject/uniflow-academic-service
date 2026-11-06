package com.uniflow.academic.task.application;

import com.uniflow.academic.task.application.services.ChangeTaskStatusService;
import com.uniflow.academic.task.domain.Task;
import com.uniflow.academic.task.domain.TaskStatus;
import com.uniflow.academic.task.domain.exception.InvalidStatusTransitionException;
import com.uniflow.academic.task.domain.exception.TaskNotFoundException;
import org.junit.jupiter.api.Test;

import static com.uniflow.academic.task.TaskFixtures.NOW;
import static com.uniflow.academic.task.TaskFixtures.aTask;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChangeTaskStatusServiceTest extends TaskServiceTestBase {

    private final ChangeTaskStatusService service = new ChangeTaskStatusService(repository, clock, reminders);

    @Test
    void changeStatusTransitionsAndSaves() {
        Task task = service.execute("t1", TaskStatus.IN_PROGRESS, USER);

        assertThat(task.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(stored("t1").getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(scheduler.cancelled).isEmpty();
    }

    @Test
    void changeStatusToCancelledCancelsTheReminder() {
        service.execute("t1", TaskStatus.CANCELLED, USER);

        assertThat(scheduler.cancelled).containsExactly("t1");
    }

    @Test
    void changeStatusToDoneCancelsTheReminderAndRecordsCompletion() {
        repository.save(aTask().status(TaskStatus.IN_REVIEW).build());

        Task task = service.execute("t1", TaskStatus.DONE, USER);

        assertThat(task.getCompletedAt()).isEqualTo(NOW);
        assertThat(scheduler.cancelled).containsExactly("t1");
    }

    @Test
    void changeStatusSurvivesASchedulerFailure() {
        scheduler.failing = true;

        service.execute("t1", TaskStatus.CANCELLED, USER);

        assertThat(stored("t1").getStatus()).isEqualTo(TaskStatus.CANCELLED);
    }

    @Test
    void invalidTransitionLeavesTheTaskUntouched() {
        assertThatThrownBy(() -> service.execute("t1", TaskStatus.DONE, USER))
                .isInstanceOf(InvalidStatusTransitionException.class);
        assertThat(stored("t1").getStatus()).isEqualTo(TaskStatus.TODO);
        assertThat(scheduler.cancelled).isEmpty();
    }

    @Test
    void changeStatusOfAnotherUsersTaskIsNotFound() {
        assertThatThrownBy(() -> service.execute("t1", TaskStatus.IN_PROGRESS, OTHER_USER))
                .isInstanceOf(TaskNotFoundException.class);
    }
}
