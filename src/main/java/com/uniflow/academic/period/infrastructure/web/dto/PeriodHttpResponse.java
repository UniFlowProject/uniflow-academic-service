package com.uniflow.academic.period.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * HTTP Response DTO for Period.
 * Maps domain Period to HTTP response format.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Academic period response")
public class PeriodHttpResponse {

    @JsonProperty("id")
    @Schema(description = "Unique period identifier")
    private String id;

    @JsonProperty("name")
    @Schema(description = "Period name (e.g., 'I Semestre')")
    private String name;

    @JsonProperty("type")
    @Schema(
            description = "Period type",
            example = "first-semester",
            allowableValues = {
                    "first-semester",
                    "second-semester",
                    "summer",
                    "special"
            }
    )
    private String type;

    @JsonProperty("year")
    @Schema(description = "Academic year", example = "2025")
    private Integer year;

    @JsonProperty("startDate")
    @Schema(
            description = "Period start date",
            example = "2025-02-03"
    )
    private LocalDate startDate;

    @JsonProperty("endDate")
    @Schema(
            description = "Period end date",
            example = "2025-06-20"
    )
    private LocalDate endDate;

    @JsonProperty("userId")
    @Schema(description = "User ID who owns this period")
    private String userId;

    @JsonProperty("isActive")
    @Schema(description = "Whether this period is currently active")
    private Boolean isActive;

    @JsonProperty("createdAt")
    @Schema(description = "Period creation timestamp")
    private LocalDateTime createdAt;

    @JsonProperty("updatedAt")
    @Schema(description = "Period last update timestamp")
    private LocalDateTime updatedAt;
}