package com.uniflow.academic.period.application.services;

import com.uniflow.academic.period.application.ports.in.CreatePeriodCommand;
import com.uniflow.academic.period.application.ports.out.PeriodRepository;
import com.uniflow.academic.period.domain.exception.InvalidPeriodException;
import com.uniflow.academic.period.domain.Period;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CreatePeriodService implements CreatePeriodCommand {

    private final PeriodRepository periodRepository;

    @Override
    public Period execute(CreatePeriodRequest request, String userId) {
        log.info(
                "Creating new period: {} for user: {}",
                request.name(),
                userId
        );

        Period period = Period.create(
                request.name(),
                parsePeriodType(request.type()),
                request.year(),
                request.startDate(),
                request.endDate(),
                userId
        );

        Period savedPeriod = periodRepository.save(period);
        log.info("Period created successfully with id: {}", savedPeriod.getId());

        return savedPeriod;
    }

    private Period.PeriodType parsePeriodType(String type) {
        try {
            return Period.PeriodType.fromValue(type);
        } catch (IllegalArgumentException e) {
            throw new InvalidPeriodException("Invalid period type: " + type);
        }
    }
}
