package com.uniflow.academic.period.application.ports.in;

import com.uniflow.academic.period.application.ports.out.dto.PeriodFilter;
import com.uniflow.academic.period.domain.Period;
import com.uniflow.academic.shared.application.PagedResult;
import com.uniflow.academic.shared.application.PaginationParams;

/**
 * Query to retrieve a user's periods with filters and pagination.
 */
public interface GetAllPeriodsQuery {
    PagedResult<Period> execute(
            String userId,
            PaginationParams params,
            PeriodFilter filter
    );
}
