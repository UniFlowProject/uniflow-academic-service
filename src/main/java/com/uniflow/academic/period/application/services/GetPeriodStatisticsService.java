package com.uniflow.academic.period.application.services;

import com.uniflow.academic.period.application.ports.in.GetPeriodStatisticsQuery;
import com.uniflow.academic.period.application.ports.out.PeriodRepository;
import com.uniflow.academic.period.application.ports.out.dto.PeriodStatisticsResponse;
import com.uniflow.academic.period.domain.Period;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.LocalDate;
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
    private final Clock clock;

    @Override
    public PeriodStatisticsResponse execute(String userId) {
        log.info("Calculating period statistics for user: {}", userId);

        List<Period> allPeriods = periodRepository.findAllByUserId(userId);

        long total = allPeriods.size();
        LocalDate today = LocalDate.now(clock);

        long active = allPeriods.stream()
                .filter(Period::getIsActive)
                .count();

        long current = allPeriods.stream()
                .filter(p -> !p.getStartDate().isAfter(today) && !p.getEndDate().isBefore(today))
                .count();

        long upcoming = allPeriods.stream()
                .filter(p -> p.getStartDate().isAfter(today))
                .count();

        long finished = allPeriods.stream()
                .filter(p -> p.getEndDate().isBefore(today))
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