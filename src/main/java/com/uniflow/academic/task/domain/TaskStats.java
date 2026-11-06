package com.uniflow.academic.task.domain;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Aggregated counts of a user's tasks. Breakdown maps only contain keys with a positive count.
 */
public record TaskStats(
        long total,
        long completed,
        long pending,
        long overdue,
        Map<String, Long> byStatus,
        Map<String, Long> byPriority,
        Map<String, Long> byType,
        Map<String, Long> byCourse
) {

    public static TaskStats from(Collection<Task> tasks, Instant now) {
        return new TaskStats(
                tasks.size(),
                tasks.stream().filter(t -> t.getStatus() == TaskStatus.DONE).count(),
                tasks.stream().filter(Task::canBeModified).count(),
                tasks.stream().filter(t -> t.isOverdue(now)).count(),
                countBy(tasks, t -> t.getStatus().value()),
                countBy(tasks, t -> t.getPriority().value()),
                countBy(tasks, t -> t.getType().value()),
                countBy(tasks, Task::getCourseId)
        );
    }

    private static Map<String, Long> countBy(Collection<Task> tasks, Function<Task, String> key) {
        return tasks.stream().collect(Collectors.groupingBy(key, Collectors.counting()));
    }
}
