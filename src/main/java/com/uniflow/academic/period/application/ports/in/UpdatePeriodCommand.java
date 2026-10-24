package com.uniflow.academic.period.application.ports.in;

import com.uniflow.academic.period.application.ports.out.dto.UpdatePeriodRequest;
import com.uniflow.academic.period.domain.Period;

public interface UpdatePeriodCommand {
    /**
     * Update period information
     *
     * @param periodId The period to update
     * @param request Contains updated period details
     * @param userId The user ID making the request
     * @return The updated Period
     */
    Period execute(
            String periodId,
            UpdatePeriodRequest request,
            String userId
    );
}
