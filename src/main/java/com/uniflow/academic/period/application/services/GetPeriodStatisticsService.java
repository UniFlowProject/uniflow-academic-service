package com.uniflow.academic.period.application.services;

import com.uniflow.academic.period.application.ports.in.GetPeriodStatisticsQuery;
import com.uniflow.academic.period.application.ports.out.PeriodRepository;
import com.uniflow.academic.shared.application.PaginationParams;
import com.uniflow.academic.period.application.ports.out.dto.PeriodStatisticsResponse;
import com.uniflow.academic.period.domain.Period;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service implementation for period statistics.
 * Use case: Calculate and retrieve statistics about a user's periods
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetPeriodStatisticsService implements GetPeriodStatisticsQuery {

    private final PeriodRepository periodRepository;

    @Override
    public PeriodStatisticsResponse execute(String userId) {
        log.info("Calculating period statistics for user: {}", userId);

        // Fetch all periods (use pagination internally or get all)
        List<Period> allPeriods = periodRepository.findAll(
                userId,
                new PaginationParams(),
                null
        ).items();

        long total = allPeriods.size();
        LocalDateTime now = LocalDateTime.now();

        // Count statistics
        long active = allPeriods.stream()
                .filter(Period::getIsActive)
                .count();

        long current = allPeriods.stream()
                .filter(p -> p.getStartDate().isBefore(now.toLocalDate())
                        && p.getEndDate().isAfter(now.toLocalDate()))
                .count();

        long upcoming = allPeriods.stream()
                .filter(p -> p.getStartDate().isAfter(now.toLocalDate()))
                .count();

        long finished = allPeriods.stream()
                .filter(p -> p.getEndDate().isBefore(now.toLocalDate()))
                .count();

        // Group by type
        Map<String, Long> byType = allPeriods.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getType().getValue(),
                        Collectors.counting()
                ));

        // Calculate average duration in days
        double averageDuration = allPeriods.stream()
                .mapToLong(p -> ChronoUnit.DAYS.between(
                        p.getStartDate(),
                        p.getEndDate()
                ))
                .average()
                .orElse(0.0);

        PeriodStatisticsResponse response = PeriodStatisticsResponse.builder()
                .total(total)
                .active(active)
                .current(current)
                .upcoming(upcoming)
                .finished(finished)
                .byType(byType)
                .averageDuration(Math.round(averageDuration * 100.0) / 100.0)
                .build();

        log.info(
                "Statistics calculated: total={}, active={}, current={}",
                total,
                active,
                current
        );

        return response;
    }
}