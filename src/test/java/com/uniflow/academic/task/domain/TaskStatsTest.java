package com.uniflow.academic.task.domain;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static com.uniflow.academic.task.TaskFixtures.NOW;
import static com.uniflow.academic.task.TaskFixtures.aTask;
import static org.assertj.core.api.Assertions.assertThat;

class TaskStatsTest {

    @Test
    void statsAreComputedFromTheTasks() {
        List<Task> tasks = List.of(
                aTask().id("1").status(TaskStatus.DONE).courseId("a").build(),
                aTask().id("2").status(TaskStatus.TODO).courseId("a").dueDate(NOW.minus(Duration.ofDays(1))).build(),
                aTask().id("3").status(TaskStatus.IN_PROGRESS).courseId("b").priority(TaskPriority.HIGH).build(),
                aTask().id("4").status(TaskStatus.CANCELLED).courseId("b").build()
        );

        TaskStats stats = TaskStats.from(tasks, NOW);

        assertThat(stats.total()).isEqualTo(4);
        assertThat(stats.completed()).isEqualTo(1);
        assertThat(stats.pending()).isEqualTo(2);
        assertThat(stats.overdue()).isEqualTo(1);
        assertThat(stats.byStatus()).containsEntry("todo", 1L).containsEntry("done", 1L)
                .doesNotContainKey("in-review");
        assertThat(stats.byPriority()).containsEntry("high", 1L).containsEntry("medium", 3L);
        assertThat(stats.byType()).isEqualTo(Map.of("assignment", 4L));
        assertThat(stats.byCourse()).isEqualTo(Map.of("a", 2L, "b", 2L));
    }

    @Test
    void emptyStatsHaveZeroCountsAndNoKeys() {
        TaskStats stats = TaskStats.from(List.of(), NOW);

        assertThat(stats.total()).isZero();
        assertThat(stats.completed()).isZero();
        assertThat(stats.pending()).isZero();
        assertThat(stats.overdue()).isZero();
        assertThat(stats.byStatus()).isEmpty();
        assertThat(stats.byCourse()).isEmpty();
    }
}
