package com.uniflow.academic.task.application.ports.out.dto;

import com.uniflow.academic.task.domain.TaskPriority;
import com.uniflow.academic.task.domain.TaskStatus;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.List;

/**
 * Optional listing criteria. Empty status/priority lists mean "no filter"; overdue and dueSoon are
 * evaluated against the {@code now} given to the repository.
 */
@Value
@Builder(toBuilder = true)
public class TaskFilter {
    @Builder.Default
    List<TaskStatus> statuses = List.of();
    @Builder.Default
    List<TaskPriority> priorities = List.of();
    String courseId;
    String periodId;
    Instant dueDateFrom;
    Instant dueDateTo;
    Boolean overdue;
    Boolean dueSoon;
    String search;
    @Builder.Default
    TaskSortField sortBy = TaskSortField.DUE_DATE;
    @Builder.Default
    SortOrder sortOrder = SortOrder.ASC;
}
