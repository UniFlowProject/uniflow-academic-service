package com.uniflow.academic.period.application.ports.out.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDate;

/**
 * DTO for updating an existing period.
 * All fields are optional - only provided fields will be updated.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class UpdatePeriodRequest {

    @Size(min = 1, max = 255, message = "Period name must be between 1 and 255 characters")
    @JsonProperty("name")
    private String name;

    @Pattern(
            regexp = "first-semester|second-semester|summer|special",
            message = "Period type must be one of: first-semester, second-semester, summer, special"
    )
    @JsonProperty("type")
    private String type;

    @Min(value = 1900, message = "Year must be at least 1900")
    @Max(value = 2100, message = "Year must not exceed 2100")
    @JsonProperty("year")
    private Integer year;

    @JsonProperty("startDate")
    private LocalDate startDate;

    @JsonProperty("endDate")
    private LocalDate endDate;

    /**
     * Check if any field has been set for update
     */
    public boolean hasAnyField() {
        return name != null
                || type != null
                || year != null
                || startDate != null
                || endDate != null;
    }

    /**
     * Create a new request with only the non-null fields merged into the existing one
     */
    public UpdatePeriodRequest mergeWith(UpdatePeriodRequest other) {
        if (other == null) {
            return this;
        }
        return UpdatePeriodRequest.builder()
                .name(other.name != null ? other.name : this.name)
                .type(other.type != null ? other.type : this.type)
                .year(other.year != null ? other.year : this.year)
                .startDate(other.startDate != null ? other.startDate : this.startDate)
                .endDate(other.endDate != null ? other.endDate : this.endDate)
                .build();
    }
}
