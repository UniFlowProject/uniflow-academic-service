package com.uniflow.academic.period.application.ports.in;

import com.uniflow.academic.period.domain.Period;

/**
 * Command to activate a period (deactivates others for the user).
 */
public interface ActivatePeriodCommand {
    /**
     * Activate a period and deactivate all others for this user
     *
     * @param periodId The period to activate
     * @param userId The user ID making the request
     * @return The activated Period
     */
    Period execute(String periodId, String userId);
}
