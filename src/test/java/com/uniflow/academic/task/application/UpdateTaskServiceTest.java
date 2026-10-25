package com.uniflow.academic.task.application;

import com.uniflow.academic.course.domain.exception.CourseNotFoundException;
import com.uniflow.academic.task.application.services.UpdateTaskService;
import com.uniflow.academic.task.domain.Task;
import com.uniflow.academic.task.domain.TaskChanges;
import com.uniflow.academic.task.domain.TaskStatus;
import com.uniflow.academic.task.domain.exception.InvalidTaskException;
import com.uniflow.academic.task.domain.exception.TaskNotFoundException;
import com.uniflow.academic.task.domain.exception.TaskNotModifiableException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static com.uniflow.academic.task.TaskFixtures.NOW;
import static com.uniflow.academic.task.TaskFixtures.aTask;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UpdateTaskServiceTest extends TaskServiceTestBase {

    private final UpdateTaskService service = new UpdateTaskService(repository, parents, clock, reminders);

    private TaskChanges.TaskChangesBuilder changes() {
        return TaskChanges.builder();
    }

    @Test
    void updateChangesOnlyTheGivenFields() {
        Task updated = service.execute("t1", changes().description("Details").build(), USER);

        assertThat(updated.getDescription()).isEqualTo("Details");
        assertThat(updated.getTitle()).isEqualTo("Homework");
        assertThat(updated.getCourseId()).isEqualTo("c1");
        assertThat(stored("t1")).isEqualTo(updated);
        assertThat(scheduler.rescheduled).isEmpty();
    }

    @Test
    void updateReschedulesTheReminderWhenTheTitleChanges() {
        Task updated = service.execute("t1", changes().title("Renamed").build(), USER);

        assertThat(scheduler.rescheduled).containsExactly(updated);
    }

    @Test
    void updateWithTheSameTitleDoesNotReschedule() {
        service.execute("t1", changes().title("Homework").build(), USER);

        assertThat(scheduler.rescheduled).isEmpty();
    }

    @Test
    void updateReschedulesTheReminderWhenTheDueDateChanges() {
        Task updated = service.execute("t1", changes().dueDate(NOW.plus(Duration.ofDays(10))).build(), USER);

        assertThat(scheduler.rescheduled).containsExactly(updated);
    }

    @Test
    void updateWithTheSameDueDateDoesNotReschedule() {
        service.execute("t1", changes().dueDate(stored("t1").getDueDate()).build(), USER);

        assertThat(scheduler.rescheduled).isEmpty();
    }

    @Test
    void updateSurvivesASchedulerFailure() {
        scheduler.failing = true;

        Task updated = service.execute("t1", changes().dueDate(NOW.plus(Duration.ofDays(10))).build(), USER);

        assertThat(stored("t1")).isEqualTo(updated);
    }

    @Test
    void updateRejectsFinishedTasks() {
        repository.save(aTask().status(TaskStatus.DONE).build());

        assertThatThrownBy(() -> service.execute("t1", changes().title("x").build(), USER))
                .isInstanceOf(TaskNotModifiableException.class);
    }

    @Test
    void updateOfAnotherUsersTaskIsNotFound() {
        assertThatThrownBy(() -> service.execute("t1", changes().title("x").build(), OTHER_USER))
                .isInstanceOf(TaskNotFoundException.class);
    }

    @Test
    void updateOfAnUnknownTaskIsNotFound() {
        assertThatThrownBy(() -> service.execute("missing", changes().title("x").build(), USER))
                .isInstanceOf(TaskNotFoundException.class);
    }

    @Test
    void updateRevalidatesDomainInvariants() {
        assertThatThrownBy(() -> service.execute("t1", changes().groupMembers(List.of("u3")).build(), USER))
                .isInstanceOf(InvalidTaskException.class);
        assertThat(stored("t1").getGroupMembers()).isEmpty();
    }

    @Test
    void updateMovesTheTaskToAnotherOwnedCourseOfTheSamePeriod() {
        ownedCourse("c2", "p1");

        Task updated = service.execute("t1", changes().courseId(" c2 ").build(), USER);

        assertThat(updated.getCourseId()).isEqualTo("c2");
    }

    @Test
    void updateToAnUnknownCourseIsNotFound() {
        assertThatThrownBy(() -> service.execute("t1", changes().courseId("nope").build(), USER))
                .isInstanceOf(CourseNotFoundException.class);
    }

    @Test
    void updatingOnlyThePeriodRequiresTheCurrentCourseToBelongToIt() {
        ownedPeriod("p2");

        assertThatThrownBy(() -> service.execute("t1", changes().periodId("p2").build(), USER))
                .isInstanceOf(InvalidTaskException.class);
    }

    @Test
    void updatingCourseAndPeriodTogetherIsAccepted() {
        ownedPeriod("p2");
        ownedCourse("c2", "p2");

        Task updated = service.execute("t1", changes().periodId("p2").courseId("c2").build(), USER);

        assertThat(updated.getPeriodId()).isEqualTo("p2");
        assertThat(updated.getCourseId()).isEqualTo("c2");
    }

    @Test
    void updateCanClearTheActualHours() {
        repository.save(aTask().actualTimeHours(4).build());

        Task updated = service.execute("t1", changes().clearActualTimeHours(true).build(), USER);

        assertThat(updated.getActualTimeHours()).isNull();
    }
}
