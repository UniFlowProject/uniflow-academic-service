package com.uniflow.academic.period.application.ports.in;

import com.uniflow.academic.period.application.ports.out.dto.PeriodStatisticsResponse;

public interface GetPeriodStatisticsQuery {
    /**
     * Get period statistics including counts and breakdowns
     *
     * @param userId The user ID
     * @return Period statistics
     */
    PeriodStatisticsResponse execute(String userId);
}
