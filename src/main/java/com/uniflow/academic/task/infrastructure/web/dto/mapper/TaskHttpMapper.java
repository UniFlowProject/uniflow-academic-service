package com.uniflow.academic.task.infrastructure.web.dto.mapper;

import com.uniflow.academic.shared.application.PagedResult;
import com.uniflow.academic.task.application.ports.out.dto.SortOrder;
import com.uniflow.academic.task.application.ports.out.dto.TaskFilter;
import com.uniflow.academic.task.application.ports.out.dto.TaskSortField;
import com.uniflow.academic.task.domain.Task;
import com.uniflow.academic.task.domain.TaskChanges;
import com.uniflow.academic.task.domain.TaskDraft;
import com.uniflow.academic.task.domain.TaskPriority;
import com.uniflow.academic.task.domain.TaskStats;
import com.uniflow.academic.task.domain.TaskStatus;
import com.uniflow.academic.task.domain.TaskType;
import com.uniflow.academic.task.infrastructure.web.dto.CreateTaskHttpRequest;
import com.uniflow.academic.task.infrastructure.web.dto.TaskHttpResponse;
import com.uniflow.academic.task.infrastructure.web.dto.TaskStatsHttpResponse;
import com.uniflow.academic.task.infrastructure.web.dto.UpdateTaskHttpRequest;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.function.Function;

@Component
public class TaskHttpMapper {

    public TaskDraft toDraft(CreateTaskHttpRequest request) {
        return new TaskDraft(
                request.title(),
                request.description(),
                request.courseId(),
                request.periodId(),
                request.dueDate(),
                TaskPriority.fromValue(request.priority()),
                TaskType.fromValue(request.type()),
                request.estimatedTimeHours(),
                request.tags(),
                request.groupMembers(),
                request.attachments()
        );
    }

    public TaskChanges toChanges(UpdateTaskHttpRequest request) {
        request.requiredFieldsSetToNull().findFirst().ifPresent(field -> {
            throw new IllegalArgumentException(field + " must not be null");
        });
        return TaskChanges.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .courseId(request.getCourseId())
                .periodId(request.getPeriodId())
                .dueDate(request.getDueDate())
                .priority(request.getPriority() == null ? null : TaskPriority.fromValue(request.getPriority()))
                .type(request.getType() == null ? null : TaskType.fromValue(request.getType()))
                .estimatedTimeHours(request.getEstimatedTimeHours())
                .actualTimeHours(request.getActualTimeHours())
                .clearActualTimeHours(request.clearsActualTimeHours())
                .tags(request.getTags())
                .groupMembers(request.getGroupMembers())
                .attachments(request.getAttachments())
                .build();
    }

    public TaskFilter toFilter(
            List<String> statuses,
            List<String> priorities,
            String courseId,
            String periodId,
            Instant dueDateFrom,
            Instant dueDateTo,
            Boolean overdue,
            Boolean dueSoon,
            String search,
            String sortBy,
            String sortOrder
    ) {
        return TaskFilter.builder()
                .statuses(parseAll(statuses, TaskStatus::fromValue))
                .priorities(parseAll(priorities, TaskPriority::fromValue))
                .courseId(courseId)
                .periodId(periodId)
                .dueDateFrom(dueDateFrom)
                .dueDateTo(dueDateTo)
                .overdue(overdue)
                .dueSoon(dueSoon)
                .search(search)
                .sortBy(sortBy == null ? TaskSortField.DUE_DATE : TaskSortField.fromValue(sortBy))
                .sortOrder(sortOrder == null ? SortOrder.ASC : SortOrder.fromValue(sortOrder))
                .build();
    }

    public TaskHttpResponse toHttpResponse(Task task) {
        return TaskHttpResponse.builder()
                .id(task.getId())
                .userId(task.getUserId())
                .title(task.getTitle())
                .description(task.getDescription())
                .courseId(task.getCourseId())
                .periodId(task.getPeriodId())
                .dueDate(task.getDueDate())
                .status(task.getStatus().value())
                .priority(task.getPriority().value())
                .type(task.getType().value())
                .estimatedTimeHours(task.getEstimatedTimeHours())
                .actualTimeHours(task.getActualTimeHours())
                .tags(task.getTags())
                .groupMembers(task.getGroupMembers())
                .attachments(task.getAttachments())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .completedAt(task.getCompletedAt())
                .build();
    }

    public PagedResult<TaskHttpResponse> toPagedResponse(PagedResult<Task> result) {
        return result.map(this::toHttpResponse);
    }

    public TaskStatsHttpResponse toStatsResponse(TaskStats stats) {
        return new TaskStatsHttpResponse(
                stats.total(),
                stats.completed(),
                stats.pending(),
                stats.overdue(),
                stats.byStatus(),
                stats.byPriority(),
                stats.byType(),
                stats.byCourse()
        );
    }

    private static <T> List<T> parseAll(List<String> raw, Function<String, T> parser) {
        return raw == null ? List.of() : raw.stream().map(parser).toList();
    }
}
