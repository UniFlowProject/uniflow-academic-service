package com.uniflow.academic.period.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Request DTO para crear periodos
 * Solo acepta formato: yyyy-MM-dd
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request to create a new period")
public class CreatePeriodHttpRequest {

    @NotBlank(message = "Period name is required")
    @Size(min = 1, max = 255)
    @JsonProperty("name")
    @Schema(description = "Period name", example = "I Semestre")
    private String name;

    @NotBlank(message = "Period type is required")
    @Pattern(regexp = "first-semester|second-semester|summer|special")
    @JsonProperty("type")
    @Schema(
            description = "Period type",
            example = "first-semester"
    )
    private String type;

    @NotNull(message = "Year is required")
    @Min(1900)
    @Max(2100)
    @JsonProperty("year")
    @Schema(description = "Academic year", example = "2025")
    private Integer year;

    @NotNull(message = "Start date is required")
    @JsonProperty("startDate")
    @Schema(
            description = "Period start date",
            example = "2025-02-03",
            format = "date"
    )
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    @JsonProperty("endDate")
    @Schema(
            description = "Period end date",
            example = "2025-06-20",
            format = "date"
    )
    private LocalDate endDate;
}