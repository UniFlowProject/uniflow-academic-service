package com.uniflow.academic.period.application.services;

import com.uniflow.academic.period.application.ports.in.ActivatePeriodCommand;
import com.uniflow.academic.period.application.ports.out.PeriodRepository;
import com.uniflow.academic.period.domain.exception.PeriodNotFoundException;
import com.uniflow.academic.period.domain.Period;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service implementation for activating academic periods.
 * Use case: Activate a period (deactivates all others for the user)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ActivatePeriodService implements ActivatePeriodCommand {

    private final PeriodRepository periodRepository;

    @Override
    public Period execute(String periodId, String userId) {
        log.info(
                "Activating period: {} for user: {}",
                periodId,
                userId
        );

        // Fetch the period to activate
        Period periodToActivate =
                periodRepository.findById(periodId, userId)
                        .orElseThrow(() -> new PeriodNotFoundException(
                                "Period not found: " + periodId
                        ));

        // Deactivate all currently active periods for this user
        List<Period> activePeriods = periodRepository.findAllActive(userId);
        for (Period activePeriod : activePeriods) {
            if (!activePeriod.getId().equals(periodId)) {
                Period deactivated = activePeriod.toBuilder()
                        .isActive(false)
                        .updatedAt(LocalDateTime.now())
                        .build();
                periodRepository.update(deactivated);
                log.debug("Deactivated period: {}", activePeriod.getId());
            }
        }

        // Activate the target period
        Period activatedPeriod = periodToActivate.toBuilder()
                .isActive(true)
                .updatedAt(LocalDateTime.now())
                .build();

        Period result = periodRepository.update(activatedPeriod);
        log.info("Period activated successfully: {}", periodId);

        return result;
    }
}
