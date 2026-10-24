package com.uniflow.academic.period.application.ports.out;

import com.uniflow.academic.period.domain.Period;
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
    Optional<Period> findCurrentActive(String userId);

    /**
     * Find all active periods for a user
     *
     * @param userId The user ID
     * @return List of active periods
     */
    List<Period> findAllActive(String userId);

    /**
     * Delete a period
     *
     * @param periodId The period ID
     * @param userId The user ID (for authorization)
     */
    void delete(String periodId, String userId);

    /**
     * Check if period has associated courses
     *
     * @param periodId The period ID
     * @return true if period has courses
     */
    boolean hasAssociatedCourses(String periodId);

    /**
     * Check if period exists for user
     *
     * @param periodId The period ID
     * @param userId The user ID
     * @return true if period exists
     */
    boolean existsByIdAndUserId(String periodId, String userId);
}
