package com.uniflow.academic.period.application;

import com.uniflow.academic.period.application.ports.out.PeriodRepository;
import com.uniflow.academic.period.application.ports.out.dto.PeriodStatisticsResponse;
import com.uniflow.academic.period.application.services.GetPeriodStatisticsService;
import com.uniflow.academic.period.domain.Period;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GetPeriodStatisticsServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2025, 3, 10);
    private static final String USER = "user-1";

    private final Clock clock = Clock.fixed(Instant.parse("2025-03-10T15:30:00Z"), ZoneOffset.UTC);
    private final PeriodRepository repository = mock(PeriodRepository.class);
    private final GetPeriodStatisticsService service = new GetPeriodStatisticsService(repository, clock);

    private static Period period(Period.PeriodType type, LocalDate start, LocalDate end, boolean active) {
        return Period.builder()
                .id(start + "/" + end)
                .type(type)
                .startDate(start)
                .endDate(end)
                .isActive(active)
                .build();
    }

    private PeriodStatisticsResponse statsFor(Period... periods) {
        when(repository.findAllByUserId(USER)).thenReturn(List.of(periods));
        return service.execute(USER);
    }

    @Test
    void periodStartingTodayIsCurrent() {
        PeriodStatisticsResponse stats = statsFor(
                period(Period.PeriodType.SUMMER, TODAY, TODAY.plusDays(10), false));

        assertThat(stats.getCurrent()).isEqualTo(1);
        assertThat(stats.getUpcoming()).isZero();
        assertThat(stats.getFinished()).isZero();
    }

    @Test
    void periodEndingTodayIsCurrent() {
        PeriodStatisticsResponse stats = statsFor(
                period(Period.PeriodType.SUMMER, TODAY.minusDays(10), TODAY, false));

        assertThat(stats.getCurrent()).isEqualTo(1);
        assertThat(stats.getFinished()).isZero();
    }

    @Test
    void oneDayPeriodTodayIsCurrent() {
        PeriodStatisticsResponse stats = statsFor(
                period(Period.PeriodType.SPECIAL, TODAY, TODAY, false));

        assertThat(stats.getCurrent()).isEqualTo(1);
        assertThat(stats.getUpcoming()).isZero();
        assertThat(stats.getFinished()).isZero();
    }

    @Test
    void everyPeriodLandsInExactlyOneBucket() {
        PeriodStatisticsResponse stats = statsFor(
                period(Period.PeriodType.FIRST_SEMESTER, TODAY.minusDays(30), TODAY.minusDays(1), false),
                period(Period.PeriodType.SECOND_SEMESTER, TODAY.minusDays(5), TODAY.plusDays(5), true),
                period(Period.PeriodType.SUMMER, TODAY.plusDays(1), TODAY.plusDays(20), false),
                period(Period.PeriodType.SPECIAL, TODAY, TODAY, false));

        assertThat(stats.getTotal()).isEqualTo(4);
        assertThat(stats.getFinished()).isEqualTo(1);
        assertThat(stats.getCurrent()).isEqualTo(2);
        assertThat(stats.getUpcoming()).isEqualTo(1);
        assertThat(stats.getFinished() + stats.getCurrent() + stats.getUpcoming()).isEqualTo(stats.getTotal());
        assertThat(stats.getActive()).isEqualTo(1);
    }

    @Test
    void groupsByTypeAndAveragesDurationInDays() {
        PeriodStatisticsResponse stats = statsFor(
                period(Period.PeriodType.SUMMER, TODAY, TODAY.plusDays(10), false),
                period(Period.PeriodType.SUMMER, TODAY, TODAY.plusDays(20), false),
                period(Period.PeriodType.SPECIAL, TODAY, TODAY.plusDays(3), false));

        assertThat(stats.getByType()).containsEntry("summer", 2L).containsEntry("special", 1L);
        assertThat(stats.getAverageDuration()).isEqualTo(11.0);
    }

    @Test
    void countsMoreThanOnePageOfPeriods() {
        Period[] periods = new Period[15];
        for (int i = 0; i < periods.length; i++) {
            periods[i] = period(Period.PeriodType.SUMMER, TODAY.minusDays(100 + i), TODAY.minusDays(50), false);
        }

        assertThat(statsFor(periods).getTotal()).isEqualTo(15);
    }

    @Test
    void emptyUserHasZeroStatistics() {
        PeriodStatisticsResponse stats = statsFor();

        assertThat(stats.getTotal()).isZero();
        assertThat(stats.getAverageDuration()).isZero();
    }
}
