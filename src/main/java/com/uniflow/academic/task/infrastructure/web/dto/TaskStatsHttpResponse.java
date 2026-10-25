package com.uniflow.academic.task.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

@Schema(description = "Task counts of the authenticated user; breakdowns only contain keys with a positive count")
public record TaskStatsHttpResponse(
        long total,
        @Schema(description = "Tasks in status done") long completed,
        @Schema(description = "Tasks that can still be modified") long pending,
        long overdue,
        Map<String, Long> byStatus,
        Map<String, Long> byPriority,
        Map<String, Long> byType,
        Map<String, Long> byCourse
) {
}
