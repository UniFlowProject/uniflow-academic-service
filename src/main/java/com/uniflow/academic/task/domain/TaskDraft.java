package com.uniflow.academic.task.domain;

import java.time.Instant;
import java.util.List;

/**
 * Client-provided data needed to create a task. Null description and lists default to empty.
 */
public record TaskDraft(
        String title,
        String description,
        String courseId,
        String periodId,
        Instant dueDate,
        TaskPriority priority,
        TaskType type,
        Integer estimatedTimeHours,
        List<String> tags,
        List<String> groupMembers,
        List<String> attachments
) {
}
