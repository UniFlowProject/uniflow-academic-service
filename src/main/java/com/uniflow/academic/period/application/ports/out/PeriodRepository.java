package com.uniflow.academic.period.application.ports.out;

import com.uniflow.academic.period.domain.Period;
import com.uniflow.academic.period.domain.exception.PeriodNotFoundException;
import com.uniflow.academic.period.application.ports.out.dto.PeriodFilter;
import com.uniflow.academic.shared.application.PaginationParams;
import com.uniflow.academic.shared.application.PagedResult;
import java.util.List;
import java.util.Optional;

public interface PeriodRepository {
    /**
     * Save a new period to persistence
     *
     * @param period The period to save
     * @return The saved period with ID
     */
    Period save(Period period);

    /**
     * Update an existing period
     *
     * @param period The period to update
     * @return The updated period
     */
    Period update(Period period);

    /**
     * Find a period by ID and user ID
     *
     * @param periodId The period ID
     * @param userId The user ID (for data isolation)
     * @return Optional containing the period if found
     */
    Optional<Period> findById(String periodId, String userId);

    /**
     * Find all periods for a user with filters and pagination
     *
     * @param userId The user ID
     * @param params Pagination parameters
     * @param filter Optional filters
     * @return Paginated response
     */
    PagedResult<Period> findAll(
            String userId,
            PaginationParams params,
            PeriodFilter filter
    );

    /**
     * Find the currently active period for a user
     *
     * @param userId The user ID
     * @return Optional containing the active period if found
     */
    /**
     * All periods of a user, unpaged (for aggregations).
     */
    List<Period> findAllByUserId(String userId);

    Optional<Period> findCurrentActive(String userId);

    /**
     * Find all active periods for a user
     *
     * @param userId The user ID
     * @return List of active periods
     */
    List<Period> findAllActive(String userId);

    /**
     * Delete a period owned by the user.
     *
     * @return true if a row was deleted, false if the user does not own that period
     */
    boolean delete(String periodId, String userId);

    boolean existsByIdAndUserId(String periodId, String userId);

    /**
     * Load a period owned by the user. Ids are trimmed; blank, unknown or foreign ids all yield
     * {@link PeriodNotFoundException}, so callers never leak whether another user's period exists.
     */
    default Period requireOwned(String periodId, String userId) {
        String id = periodId == null ? "" : periodId.trim();
        if (id.isEmpty()) {
            throw new PeriodNotFoundException("Period not found: " + periodId);
        }
        return findById(id, userId)
                .orElseThrow(() -> new PeriodNotFoundException("Period not found: " + id));
    }
}
