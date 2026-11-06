package com.uniflow.academic.task.application;

import com.uniflow.academic.course.domain.exception.CourseNotFoundException;
import com.uniflow.academic.period.domain.exception.PeriodNotFoundException;
import com.uniflow.academic.task.application.services.CreateTaskService;
import com.uniflow.academic.task.domain.Task;
import com.uniflow.academic.task.domain.TaskDraft;
import com.uniflow.academic.task.domain.TaskPriority;
import com.uniflow.academic.task.domain.TaskStatus;
import com.uniflow.academic.task.domain.TaskType;
import com.uniflow.academic.task.domain.exception.InvalidTaskException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static com.uniflow.academic.task.TaskFixtures.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CreateTaskServiceTest extends TaskServiceTestBase {

    private final CreateTaskService service = new CreateTaskService(repository, parents, this::nextId, clock, reminders);

    private TaskDraft draft(String periodId, String courseId, List<String> groupMembers) {
        return new TaskDraft("Essay", null, courseId, periodId, NOW.plus(Duration.ofDays(2)),
                TaskPriority.HIGH, TaskType.ESSAY, 3, null, groupMembers, null);
    }

    @Test
    void createPersistsTheTaskAndSchedulesAReminder() {
        Task task = service.execute(draft("p1", "c1", null), USER);

        assertThat(task.getId()).isEqualTo("id-1");
        assertThat(task.getUserId()).isEqualTo(USER);
        assertThat(task.getStatus()).isEqualTo(TaskStatus.TODO);
        assertThat(task.getCreatedAt()).isEqualTo(NOW);
        assertThat(task.getUpdatedAt()).isEqualTo(NOW);
        assertThat(stored("id-1")).isEqualTo(task);
        assertThat(scheduler.scheduled).containsExactly(task);
    }

    @Test
    void createSurvivesASchedulerFailure() {
        scheduler.failing = true;

        Task task = service.execute(draft("p1", "c1", null), USER);

        assertThat(stored(task.getId())).isNotNull();
    }

    @Test
    void createUsesTheCanonicalIdsOfTheOwnedParents() {
        Task task = service.execute(draft(" p1 ", " c1 ", null), USER);

        assertThat(task.getPeriodId()).isEqualTo("p1");
        assertThat(task.getCourseId()).isEqualTo("c1");
    }

    @Test
    void createInAnUnknownOrForeignPeriodIsNotFound() {
        assertThatThrownBy(() -> service.execute(draft("other-period", "c1", null), USER))
                .isInstanceOf(PeriodNotFoundException.class);
        assertThat(repository.tasks).hasSize(1);
        assertThat(scheduler.scheduled).isEmpty();
    }

    @Test
    void createInAnUnknownOrForeignCourseIsNotFound() {
        assertThatThrownBy(() -> service.execute(draft("p1", "other-course", null), USER))
                .isInstanceOf(CourseNotFoundException.class);
        assertThat(repository.tasks).hasSize(1);
    }

    @Test
    void createWithACourseOfAnotherPeriodIsInvalid() {
        ownedPeriod("p2");

        assertThatThrownBy(() -> service.execute(draft("p2", "c1", null), USER))
                .isInstanceOf(InvalidTaskException.class);
        assertThat(repository.tasks).hasSize(1);
    }

    @Test
    void createViolatingADomainRuleSavesAndSchedulesNothing() {
        assertThatThrownBy(() -> service.execute(draft("p1", "c1", List.of("u3")), USER))
                .isInstanceOf(InvalidTaskException.class);
        assertThat(repository.tasks).hasSize(1);
        assertThat(scheduler.scheduled).isEmpty();
    }
}
