package com.uniflow.academic.period.application.services;

import com.uniflow.academic.period.application.ports.in.GetAllPeriodsQuery;
import com.uniflow.academic.period.application.ports.out.PeriodRepository;
import com.uniflow.academic.period.application.ports.out.dto.PeriodFilter;
import com.uniflow.academic.period.domain.Period;
import com.uniflow.academic.shared.application.PagedResult;
import com.uniflow.academic.shared.application.PaginationParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for retrieving all periods.
 * Use case: Get all periods for a user with optional filters and pagination
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetAllPeriodsService implements GetAllPeriodsQuery {

    private final PeriodRepository periodRepository;

    @Override
    public PagedResult<Period> execute(
            String userId,
            PaginationParams params,
            PeriodFilter filter
    ) {
        log.info(
                "Fetching periods for user: {} with page: {}, limit: {}",
                userId,
                params.page(),
                params.limit()
        );

        PagedResult<Period> response = periodRepository.findAll(
                userId,
                params,
                filter
        );

        log.info(
                "Retrieved {} periods out of {} total",
                response.items().size(),
                response.pageInfo().total()
        );

        return response;
    }
}