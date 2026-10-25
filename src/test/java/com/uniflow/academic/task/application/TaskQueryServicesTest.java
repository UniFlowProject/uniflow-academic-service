package com.uniflow.academic.task.application;

import com.uniflow.academic.shared.application.PagedResult;
import com.uniflow.academic.shared.application.PaginationParams;
import com.uniflow.academic.task.application.ports.out.dto.TaskFilter;
import com.uniflow.academic.task.application.services.GetTaskByIdService;
import com.uniflow.academic.task.application.services.GetTaskStatsService;
import com.uniflow.academic.task.application.services.ListTasksService;
import com.uniflow.academic.task.domain.Task;
import com.uniflow.academic.task.domain.TaskStats;
import com.uniflow.academic.task.domain.exception.TaskNotFoundException;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static com.uniflow.academic.task.TaskFixtures.NOW;
import static com.uniflow.academic.task.TaskFixtures.aTask;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TaskQueryServicesTest extends TaskServiceTestBase {

    @Test
    void getReturnsTheOwnedTask() {
        assertThat(new GetTaskByIdService(repository).execute("t1", USER).getId()).isEqualTo("t1");
    }

    @Test
    void getOfAnUnknownTaskIsNotFound() {
        assertThatThrownBy(() -> new GetTaskByIdService(repository).execute("missing", USER))
                .isInstanceOf(TaskNotFoundException.class)
                .hasMessageContaining("missing");
    }

    @Test
    void getOfAnotherUsersTaskIsNotFound() {
        assertThatThrownBy(() -> new GetTaskByIdService(repository).execute("t1", OTHER_USER))
                .isInstanceOf(TaskNotFoundException.class);
    }

    @Test
    void listReturnsTheUsersPageAndEvaluatesTimeFiltersAgainstTheClock() {
        TaskFilter filter = TaskFilter.builder().overdue(true).build();

        PagedResult<Task> page = new ListTasksService(repository, clock)
                .execute(USER, filter, PaginationParams.of(1, null, 20));

        assertThat(page.items()).extracting(Task::getId).containsExactly("t1");
        assertThat(page.pageInfo().total()).isEqualTo(1);
        assertThat(repository.lastFilter).isSameAs(filter);
        assertThat(repository.lastNow).isEqualTo(NOW);
    }

    @Test
    void statsCoverEveryTaskOfTheUser() {
        repository.save(aTask().id("t2").dueDate(NOW.minus(Duration.ofDays(1))).build());

        TaskStats stats = new GetTaskStatsService(repository, clock).execute(USER);

        assertThat(stats.total()).isEqualTo(2);
        assertThat(stats.pending()).isEqualTo(2);
        assertThat(stats.overdue()).isEqualTo(1);
        assertThat(new GetTaskStatsService(repository, clock).execute(OTHER_USER).total()).isZero();
    }
}
