package com.uniflow.academic.period.application.ports.in;

import com.uniflow.academic.period.domain.Period;

import java.time.LocalDate;
import java.time.LocalDateTime;

public interface CreatePeriodCommand {
    /**
     * Create a new period for the authenticated user
     *
     * @param request Contains period creation details
     * @param userId The user ID making the request
     * @return The created Period
     */
    Period execute(CreatePeriodRequest request, String userId);

    /**
     * DTO for create period request
     */
    record CreatePeriodRequest(
            String name,
            String type,
            Integer year,
            LocalDate startDate,
            LocalDate endDate
    ) {
        public CreatePeriodRequest {
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("Period name is required");
            }
            if (type == null || type.isBlank()) {
                throw new IllegalArgumentException("Period type is required");
            }
            if (year == null) {
                throw new IllegalArgumentException("Year is required");
            }
            if (startDate == null || endDate == null) {
                throw new IllegalArgumentException(
                        "Start and end dates are required"
                );
            }
        }
    }
}
