package com.uniflow.academic.task.infrastructure.persistence.postgres;

import com.uniflow.academic.task.domain.Task;
import com.uniflow.academic.task.domain.TaskPriority;
import com.uniflow.academic.task.domain.TaskStatus;
import com.uniflow.academic.task.domain.TaskType;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TaskEntityMapper {

    public TaskEntity toEntity(Task task) {
        return TaskEntity.builder()
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

    public Task toDomain(TaskEntity entity) {
        return Task.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .courseId(entity.getCourseId())
                .periodId(entity.getPeriodId())
                .dueDate(entity.getDueDate())
                .status(TaskStatus.fromValue(entity.getStatus()))
                .priority(TaskPriority.fromValue(entity.getPriority()))
                .type(TaskType.fromValue(entity.getType()))
                .estimatedTimeHours(entity.getEstimatedTimeHours())
                .actualTimeHours(entity.getActualTimeHours())
                .tags(List.copyOf(entity.getTags()))
                .groupMembers(List.copyOf(entity.getGroupMembers()))
                .attachments(List.copyOf(entity.getAttachments()))
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .completedAt(entity.getCompletedAt())
                .build();
    }
}
