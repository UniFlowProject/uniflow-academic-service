package com.uniflow.academic.period.application.ports.in;

import com.uniflow.academic.period.domain.Period;
import com.uniflow.academic.period.domain.exception.PeriodNotFoundException;

public interface GetPeriodByIdQuery {
    /**
     * Get a specific period by ID
     *
     * @param periodId The period ID
     * @param userId The user ID (for authorization)
     * @return The Period
     * @throws PeriodNotFoundException if period not found
     */
    Period execute(String periodId, String userId);
}