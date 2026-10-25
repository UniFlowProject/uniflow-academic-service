package com.uniflow.academic.task.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.List;

@Value
@Builder
public class TaskHttpResponse {

    @Schema(description = "Task identifier")
    String id;
    String userId;
    String title;
    String description;
    String courseId;
    String periodId;
    Instant dueDate;
    @Schema(allowableValues = {"todo", "in-progress", "in-review", "done", "cancelled"})
    String status;
    @Schema(allowableValues = {"low", "medium", "high", "urgent"})
    String priority;
    @Schema(allowableValues = {"assignment", "exam", "reading", "presentation", "lab", "quiz", "essay", "group-work"})
    String type;
    Integer estimatedTimeHours;
    Integer actualTimeHours;
    List<String> tags;
    List<String> groupMembers;
    List<String> attachments;
    Instant createdAt;
    Instant updatedAt;
    Instant completedAt;
}
