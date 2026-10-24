package com.uniflow.academic.period.application.services;

import com.uniflow.academic.period.application.ports.in.UpdatePeriodCommand;
import com.uniflow.academic.period.application.ports.out.PeriodRepository;
import com.uniflow.academic.period.application.ports.out.dto.UpdatePeriodRequest;
import com.uniflow.academic.period.domain.exception.PeriodNotFoundException;
import com.uniflow.academic.period.domain.exception.InvalidPeriodException;
import com.uniflow.academic.period.domain.Period;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UpdatePeriodService implements UpdatePeriodCommand {

    private final PeriodRepository periodRepository;

    @Override
    public Period execute(
            String periodId,
            UpdatePeriodRequest request,
            String userId
    ) {
        log.info(
                "Updating period: {} for user: {}",
                periodId,
                userId
        );

        if (!request.hasAnyField()) {
            throw new InvalidPeriodException(
                    "At least one field must be provided for update"
            );
        }

        Period period = periodRepository.findById(periodId, userId)
                .orElseThrow(() -> new PeriodNotFoundException(
                        "Period not found: " + periodId
                ));

        if (request.getName() != null && !request.getName().isBlank()) {
            period = updateField(period, "name", request.getName());
        }
        if (request.getType() != null) {
            period = updateField(period, "type", parsePeriodType(request.getType()));
        }
        if (request.getYear() != null) {
            period = updateField(period, "year", request.getYear());
        }
        if (request.getStartDate() != null) {
            period = updateField(period, "startDate", request.getStartDate());
        }
        if (request.getEndDate() != null) {
            period = updateField(period, "endDate", request.getEndDate());
        }

        period = updateField(period, "updatedAt", LocalDateTime.now());
        period.validate();

        Period updatedPeriod = periodRepository.update(period);
        log.info("Period updated successfully: {}", periodId);

        return updatedPeriod;
    }

    private Period.PeriodType parsePeriodType(String type) {
        try {
            return Period.PeriodType.fromValue(type);
        } catch (IllegalArgumentException e) {
            throw new InvalidPeriodException("Invalid period type: " + type);
        }
    }

    private Period updateField(
            Period period,
            String fieldName,
            Object newValue
    ) {
        return switch (fieldName) {
            case "name" -> period.toBuilder()
                    .name((String) newValue)
                    .build();
            case "type" -> period.toBuilder()
                    .type((Period.PeriodType) newValue)
                    .build();
            case "year" -> period.toBuilder()
                    .year((Integer) newValue)
                    .build();
            case "startDate" -> period.toBuilder()
                    .startDate((LocalDate) newValue)
                    .build();
            case "endDate" -> period.toBuilder()
                    .endDate((LocalDate) newValue)
                    .build();
            case "updatedAt" -> period.toBuilder()
                    .updatedAt((LocalDateTime) newValue)
                    .build();
            default -> period;
        };
    }
}
