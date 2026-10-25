package com.uniflow.academic.task.domain;

import lombok.Builder;

import java.time.Instant;
import java.util.List;

/**
 * Partial update of a task: null fields stay unchanged. Status is not changeable here.
 * {@code actualTimeHours} is the only optional field, so clearing it needs an explicit flag.
 */
@Builder(toBuilder = true)
public record TaskChanges(
        String title,
        String description,
        String courseId,
        String periodId,
        Instant dueDate,
        TaskPriority priority,
        TaskType type,
        Integer estimatedTimeHours,
        Integer actualTimeHours,
        boolean clearActualTimeHours,
        List<String> tags,
        List<String> groupMembers,
        List<String> attachments
) {
}
