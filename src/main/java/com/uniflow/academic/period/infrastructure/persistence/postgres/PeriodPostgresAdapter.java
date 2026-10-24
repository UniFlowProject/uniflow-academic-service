package com.uniflow.academic.period.infrastructure.persistence.postgres;

import com.uniflow.academic.period.application.ports.out.PeriodRepository;
import com.uniflow.academic.period.application.ports.out.dto.PeriodFilter;
import com.uniflow.academic.shared.application.PaginationParams;
import com.uniflow.academic.period.domain.Period;
import com.uniflow.academic.shared.application.PagedResult;
import com.uniflow.academic.course.infrastructure.persistence.postgres.CourseJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class PeriodPostgresAdapter implements PeriodRepository {

    private final PeriodJpaRepository jpaRepository;
    private final PeriodEntityMapper mapper;
    private final CourseJpaRepository courseJpaRepository;

    @Override
    public Period save(Period period) {
        log.debug("Saving period: {}", period.getId());
        PeriodEntity entity = mapper.toEntity(period);
        PeriodEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Period update(Period period) {
        log.debug("Updating period: {}", period.getId());
        PeriodEntity entity = mapper.toEntity(period);
        // Flush so a deactivation reaches the database before a later activation (partial unique index).
        PeriodEntity updated = jpaRepository.saveAndFlush(entity);
        return mapper.toDomain(updated);
    }

    @Override
    public Optional<Period> findById(String periodId, String userId) {
        log.debug(
                "Finding period: {} for user: {}",
                periodId,
                userId
        );
        return jpaRepository.findByIdAndUserId(periodId, userId)
                .map(mapper::toDomain);
    }

    @Override
    public PagedResult<Period> findAll(
            String userId,
            PaginationParams params,
            PeriodFilter filter
    ) {
        log.debug(
                "Finding all periods for user: {} with filters: {}",
                userId,
                filter
        );

        params.validate();
        Pageable pageable = PageRequest.of(
                params.getPage() - 1,
                params.getLimit()
        );

        Page<PeriodEntity> page;

        // Build query based on filters
        if (filter != null && !filter.isEmpty()) {
            page = jpaRepository.findAllWithFilters(
                    userId,
                    filter.getType(),
                    filter.getYear(),
                    filter.getIsActive(),
                    pageable
            );
        } else {
            page = jpaRepository.findByUserIdOrderByStartDateDesc(userId, pageable);
        }

        List<Period> periods = page.getContent().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());

        return PagedResult.of(
                periods,
                page.getTotalElements(),
                params.getPage(),
                params.getLimit()
        );
    }

    @Override
    public List<Period> findAllByUserId(String userId) {
        return jpaRepository.findAllByUserId(userId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Period> findCurrentActive(String userId) {
        log.debug("Finding current active period for user: {}", userId);
        return jpaRepository.findByUserIdAndIsActive(userId, true)
                .map(mapper::toDomain);
    }

    @Override
    public List<Period> findAllActive(String userId) {
        log.debug("Finding all active periods for user: {}", userId);
        return jpaRepository.findAllByUserIdAndIsActive(userId, true)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(String periodId, String userId) {
        log.debug(
                "Deleting period: {} for user: {}",
                periodId,
                userId
        );
        jpaRepository.deleteById(periodId);
    }

    @Override
    public boolean hasAssociatedCourses(String periodId) {
        log.debug("Checking courses for period: {}", periodId);
        return courseJpaRepository.countByPeriodId(periodId) > 0;
    }

    @Override
    public boolean existsByIdAndUserId(String periodId, String userId) {
        log.debug(
                "Checking existence of period: {} for user: {}",
                periodId,
                userId
        );
        return jpaRepository.existsByIdAndUserId(periodId, userId);
    }
}