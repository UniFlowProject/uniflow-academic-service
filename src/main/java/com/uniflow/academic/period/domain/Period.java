package com.uniflow.academic.period.domain;

import com.uniflow.academic.period.domain.exception.InvalidPeriodException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class Period {
    private String id;
    private String name;
    private PeriodType type;
    private Integer year;
    private LocalDate startDate;
    private LocalDate endDate;
    private String userId;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Getter
    public enum PeriodType {
        FIRST_SEMESTER("first-semester"),
        SECOND_SEMESTER("second-semester"),
        SUMMER("summer"),
        SPECIAL("special");

        private final String value;

        PeriodType(String value) {
            this.value = value;
        }

        public static PeriodType fromValue(String value) {
            for (PeriodType type : PeriodType.values()) {
                if (type.value.equals(value)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Invalid period type: " + value);
        }
    }

    /**
     * Validates period business rules
     */
    public void validate() {
        if (startDate == null || endDate == null) {
            throw new InvalidPeriodException(
                    "Start date and end date are required"
            );
        }
        if (startDate.isAfter(endDate)) {
            throw new InvalidPeriodException(
                    "Start date must not be after end date"
            );
        }
        if (year == null || year < 1900 || year > 2100) {
            throw new InvalidPeriodException(
                    "Year must be between 1900 and 2100"
            );
        }
        if (name == null || name.isBlank()) {
            throw new InvalidPeriodException("Period name is required");
        }
    }

    /**
     * Create a new period with generated ID and timestamps
     */
    public static Period create(
            String name,
            PeriodType type,
            Integer year,
            LocalDate startDate,
            LocalDate endDate,
            String userId
    ) {
        Period period = Period.builder()
                .id(UUID.randomUUID().toString())
                .name(name)
                .type(type)
                .year(year)
                .startDate(startDate)
                .endDate(endDate)
                .userId(userId)
                .isActive(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        period.validate();
        return period;
    }
}
