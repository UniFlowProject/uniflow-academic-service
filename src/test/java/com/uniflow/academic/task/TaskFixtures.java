package com.uniflow.academic.task;

import com.uniflow.academic.task.domain.Task;
import com.uniflow.academic.task.domain.TaskPriority;
import com.uniflow.academic.task.domain.TaskStatus;
import com.uniflow.academic.task.domain.TaskType;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

public final class TaskFixtures {

    public static final Instant NOW = Instant.parse("2026-10-23T12:00:00Z");

    private TaskFixtures() {
    }

    public static Task.TaskBuilder aTask() {
        return Task.builder()
                .id("t1")
                .userId("u1")
                .title("Homework")
                .description("")
                .courseId("c1")
                .periodId("p1")
                .dueDate(NOW.plus(Duration.ofDays(3)))
                .status(TaskStatus.TODO)
                .priority(TaskPriority.MEDIUM)
                .type(TaskType.ASSIGNMENT)
                .estimatedTimeHours(2)
                .tags(List.of())
                .groupMembers(List.of())
                .attachments(List.of())
                .createdAt(NOW)
                .updatedAt(NOW);
    }
}
