package com.uniflow.academic.period.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * HTTP Request DTO for updating periods.
 * Used by PUT /periods/{periodId} endpoint.
 * All fields are optional.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request to update a period")
public class UpdatePeriodHttpRequest {

    @Size(min = 1, max = 255)
    @Pattern(regexp = ".*\\S.*", message = "Period name must not be blank")
    @JsonProperty("name")
    @Schema(description = "Period name", example = "I Semestre Actualizado")
    private String name;

    @Pattern(
            regexp = "first-semester|second-semester|summer|special",
            message = "Invalid period type"
    )
    @JsonProperty("type")
    @Schema(description = "Period type", example = "first-semester")
    private String type;

    @Min(1900)
    @Max(2100)
    @JsonProperty("year")
    @Schema(description = "Academic year", example = "2025")
    private Integer year;

    @JsonProperty("startDate")
    @Schema(description = "Period start date", example = "2025-02-03")
    private LocalDate startDate;

    @JsonProperty("endDate")
    @Schema(description = "Period end date", example = "2025-06-20")
    private LocalDate endDate;
}