package com.uniflow.academic.task.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

@Schema(description = "Request to create a task")
public record CreateTaskHttpRequest(
        @Schema(description = "Task title", example = "Essay on cell biology")
        @NotBlank @Size(max = 255)
        String title,

        @Schema(description = "Task description, empty by default")
        String description,

        @Schema(description = "Course the task belongs to")
        @NotBlank
        String courseId,

        @Schema(description = "Period of that course")
        @NotBlank
        String periodId,

        @Schema(description = "Due date (ISO-8601)", example = "2026-11-01T10:00:00Z")
        @NotNull
        Instant dueDate,

        @Schema(description = "Priority", allowableValues = {"low", "medium", "high", "urgent"})
        @NotNull @Pattern(regexp = TaskPatterns.PRIORITY)
        String priority,

        @Schema(description = "Task type",
                allowableValues = {"assignment", "exam", "reading", "presentation", "lab", "quiz", "essay", "group-work"})
        @NotNull @Pattern(regexp = TaskPatterns.TYPE)
        String type,

        @Schema(description = "Estimated hours", example = "4")
        @NotNull @PositiveOrZero
        Integer estimatedTimeHours,

        List<String> tags,

        @Schema(description = "Only allowed for group-work tasks")
        List<String> groupMembers,

        List<String> attachments
) {
}
