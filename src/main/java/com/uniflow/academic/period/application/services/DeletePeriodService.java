package com.uniflow.academic.period.application.services;

import com.uniflow.academic.period.application.ports.in.DeletePeriodCommand;
import com.uniflow.academic.period.application.ports.out.PeriodRepository;
import com.uniflow.academic.period.domain.exception.PeriodDeletionException;
import com.uniflow.academic.period.domain.exception.PeriodNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for deleting academic periods.
 * Use case: Delete a period if it has no associated courses
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class DeletePeriodService implements DeletePeriodCommand {

    private final PeriodRepository periodRepository;

    @Override
    public void execute(String periodId, String userId) {
        log.info(
                "Deleting period: {} for user: {}",
                periodId,
                userId
        );

        // Verify period exists and belongs to user
        if (!periodRepository.existsByIdAndUserId(periodId, userId)) {
            throw new PeriodNotFoundException(
                    "Period not found: " + periodId
            );
        }

        // Check if period has associated courses
        if (periodRepository.hasAssociatedCourses(periodId)) {
            throw new PeriodDeletionException(
                    "Cannot delete period with associated courses"
            );
        }

        periodRepository.delete(periodId, userId);
        log.info("Period deleted successfully: {}", periodId);
    }
}
