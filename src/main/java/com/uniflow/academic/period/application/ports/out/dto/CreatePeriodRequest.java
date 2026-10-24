package com.uniflow.academic.period.application.ports.out.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for creating a new period.
 * Validates all input data according to business rules.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class CreatePeriodRequest {

    @NotBlank(message = "Period name is required")
    @Size(min = 1, max = 255, message = "Period name must be between 1 and 255 characters")
    private String name;

    @NotBlank(message = "Period type is required")
    @Pattern(
            regexp = "first-semester|second-semester|summer|special",
            message = "Period type must be one of: first-semester, second-semester, summer, special"
    )
    private String type;

    @NotNull(message = "Year is required")
    @Min(value = 1900, message = "Year must be at least 1900")
    @Max(value = 2100, message = "Year must not exceed 2100")
    private Integer year;

    @NotNull(message = "Start date is required")
    @JsonProperty("startDate")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    @JsonProperty("endDate")
    private LocalDate endDate;
}
