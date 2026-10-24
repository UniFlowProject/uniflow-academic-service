package com.uniflow.academic.period.application.ports.in;

public interface DeletePeriodCommand {
    /**
     * Delete a period if it has no associated courses
     *
     * @param periodId The period to delete
     * @param userId The user ID making the request
     * @throws PeriodDeletionException if period has associated courses
     */
    void execute(String periodId, String userId);
}
