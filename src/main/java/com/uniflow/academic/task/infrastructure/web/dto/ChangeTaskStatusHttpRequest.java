package com.uniflow.academic.task.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Request to change the status of a task")
public record ChangeTaskStatusHttpRequest(
        @Schema(allowableValues = {"todo", "in-progress", "in-review", "done", "cancelled"})
        @NotNull @Pattern(regexp = TaskPatterns.STATUS)
        String status
) {
}
