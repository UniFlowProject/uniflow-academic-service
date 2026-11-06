package com.uniflow.academic.period.application.services;

import com.uniflow.academic.period.application.ports.in.GetPeriodByIdQuery;
import com.uniflow.academic.period.application.ports.out.PeriodRepository;
import com.uniflow.academic.period.domain.exception.PeriodNotFoundException;
import com.uniflow.academic.period.domain.Period;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for retrieving a specific period by ID.
 * Use case: Get a specific period's details
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetPeriodByIdService implements GetPeriodByIdQuery {

    private final PeriodRepository periodRepository;

    @Override
    public Period execute(String periodId, String userId) {
        log.info(
                "Fetching period: {} for user: {}",
                periodId,
                userId
        );

        Period period = periodRepository.findById(periodId, userId)
                .orElseThrow(() -> {
                    log.warn(
                            "Period not found: {} for user: {}",
                            periodId,
                            userId
                    );
                    return new PeriodNotFoundException(
                            "Period not found: " + periodId
                    );
                });

        log.debug("Period found: {}", periodId);
        return period;
    }
}