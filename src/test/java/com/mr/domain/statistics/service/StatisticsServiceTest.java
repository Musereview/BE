package com.mr.domain.statistics.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.mr.domain.statistics.dto.res.StatisticsResponseDTO;
import com.mr.domain.statistics.entity.PracticeStatistics;
import com.mr.domain.statistics.entity.SkillStatistics;
import com.mr.domain.statistics.entity.enums.PeriodType;
import com.mr.domain.statistics.entity.enums.SkillType;
import com.mr.domain.statistics.repository.PracticeStatisticsRepository;
import com.mr.domain.statistics.repository.SkillStatisticsRepository;
import com.mr.domain.user.entity.User;
import com.mr.domain.user.exception.UserErrorStatus;
import com.mr.domain.user.repository.UserRepository;
import com.mr.global.apipayload.exception.GeneralException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StatisticsServiceTest {
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-07-26T15:30:00Z"), ZoneId.of("Asia/Seoul"));
    private static final LocalDate THIS_WEEK = LocalDate.of(2026, 7, 27);

    @Mock UserRepository userRepository;
    @Mock PracticeStatisticsRepository practiceRepository;
    @Mock SkillStatisticsRepository skillRepository;
    private StatisticsService service;

    @BeforeEach
    void setUp() {
        service = new StatisticsService(userRepository, practiceRepository, skillRepository, CLOCK);
    }

    private void stubEmpty() {
        given(userRepository.findById(1L)).willReturn(Optional.of(mock(User.class)));
        given(practiceRepository.findAllByUser_UserIdAndPeriodTypeAndPeriodStartBetween(
                1L, PeriodType.WEEKLY, THIS_WEEK.minusWeeks(3), THIS_WEEK)).willReturn(List.of());
        given(skillRepository.findAllByUser_UserIdAndPeriodTypeAndPeriodStartBetween(
                1L, PeriodType.WEEKLY, THIS_WEEK.minusWeeks(1), THIS_WEEK)).willReturn(List.of());
    }

    private PracticeStatistics practice(LocalDate start, int minutes, int count, BigDecimal accuracy) {
        PracticeStatistics statistics = mock(PracticeStatistics.class);
        lenient().when(statistics.getPeriodStart()).thenReturn(start);
        lenient().when(statistics.getPracticeMinutes()).thenReturn(minutes);
        lenient().when(statistics.getSessionCount()).thenReturn(count);
        lenient().when(statistics.getAverageAccuracy()).thenReturn(accuracy);
        return statistics;
    }

    private SkillStatistics skill(LocalDate start, SkillType type, BigDecimal score) {
        SkillStatistics statistics = mock(SkillStatistics.class);
        when(statistics.getPeriodStart()).thenReturn(start);
        when(statistics.getSkillType()).thenReturn(type);
        when(statistics.getScore()).thenReturn(score);
        return statistics;
    }

    @Test
    void userNotFound() {
        given(userRepository.findById(1L)).willReturn(Optional.empty());
        assertThatThrownBy(() -> service.getStatistics(1L))
                .isInstanceOf(GeneralException.class)
                .hasFieldOrPropertyWithValue("code", UserErrorStatus.USER_NOT_FOUND);
    }

    @Test
    void noAggregateDataReturnsZeros() {
        stubEmpty();
        StatisticsResponseDTO response = service.getStatistics(1L);
        assertThat(response.weeklySummary().accuracy()).isEqualByComparingTo("0.0");
        assertThat(response.weeklySummary().practiceMinutes()).isZero();
        assertThat(response.weeklySummary().completedSessionCount()).isZero();
        assertThat(response.domainGrowth()).hasSize(SkillType.values().length)
                .allSatisfy(item -> assertThat(item.diff()).isEqualByComparingTo("0.0"));
        assertThat(response.weeklyTrend().items()).hasSize(4)
                .allSatisfy(item -> assertThat(item.averageScore()).isEqualByComparingTo("0.0"));
    }

    @Test
    void aggregatesBuildSameResponseContract() {
        given(userRepository.findById(1L)).willReturn(Optional.of(mock(User.class)));
        List<PracticeStatistics> practices = List.of(
                practice(THIS_WEEK.minusWeeks(3), 5, 1, new BigDecimal("60.00")),
                practice(THIS_WEEK.minusWeeks(2), 10, 1, new BigDecimal("78.00")),
                practice(THIS_WEEK.minusWeeks(1), 20, 1, new BigDecimal("63.00")),
                practice(THIS_WEEK, 20, 2, new BigDecimal("93.00")));
        List<SkillStatistics> skills = List.of(
                skill(THIS_WEEK.minusWeeks(1), SkillType.SCALE, new BigDecimal("77.00")),
                skill(THIS_WEEK, SkillType.SCALE, new BigDecimal("85.00")));
        given(practiceRepository.findAllByUser_UserIdAndPeriodTypeAndPeriodStartBetween(
                1L, PeriodType.WEEKLY, THIS_WEEK.minusWeeks(3), THIS_WEEK)).willReturn(practices);
        given(skillRepository.findAllByUser_UserIdAndPeriodTypeAndPeriodStartBetween(
                1L, PeriodType.WEEKLY, THIS_WEEK.minusWeeks(1), THIS_WEEK)).willReturn(skills);

        StatisticsResponseDTO response = service.getStatistics(1L);

        assertThat(response.weeklySummary().accuracy()).isEqualByComparingTo("93.0");
        assertThat(response.weeklySummary().accuracy().scale()).isEqualTo(1);
        assertThat(response.weeklySummary().practiceMinutesDiff()).isZero();
        assertThat(response.weeklySummary().completedSessionCountDiff()).isEqualTo(1);
        assertThat(response.domainGrowth().get(0).diff()).isEqualByComparingTo("8.0");
        assertThat(response.weeklyTrend().items()).extracting(item -> item.averageScore())
                .containsExactly(new BigDecimal("60.0"), new BigDecimal("78.0"),
                        new BigDecimal("63.0"), new BigDecimal("93.0"));
        assertThat(response.weeklyTrend().diffFromPreviousWeek()).isEqualTo(30);
    }

    @Test
    void missingPreviousScoreKeepsDiffAtZero() {
        stubEmpty();
        List<SkillStatistics> skills = List.of(
                skill(THIS_WEEK, SkillType.SCALE, new BigDecimal("85.00")));
        given(skillRepository.findAllByUser_UserIdAndPeriodTypeAndPeriodStartBetween(
                1L, PeriodType.WEEKLY, THIS_WEEK.minusWeeks(1), THIS_WEEK)).willReturn(skills);
        StatisticsResponseDTO response = service.getStatistics(1L);
        assertThat(response.domainGrowth().get(0).currentScore()).isEqualByComparingTo("85.0");
        assertThat(response.domainGrowth().get(0).diff()).isEqualByComparingTo("0.0");
    }
}
