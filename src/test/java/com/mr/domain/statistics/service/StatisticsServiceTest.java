package com.mr.domain.statistics.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.mr.domain.analysis.entity.Analysis;
import com.mr.domain.analysis.entity.enums.AnalysisStatus;
import com.mr.domain.analysis.repository.AnalysisRepository;
import com.mr.domain.playing.entity.Playing;
import com.mr.domain.playing.entity.enums.PlayingStatus;
import com.mr.domain.playing.repository.PlayingRepository;
import com.mr.domain.statistics.dto.res.StatisticsResponseDTO;
import com.mr.domain.statistics.dto.res.StatisticsResponseDTO.DomainGrowth;
import com.mr.domain.statistics.dto.res.StatisticsResponseDTO.TrendItem;
import com.mr.domain.statistics.entity.enums.SkillType;
import com.mr.domain.user.entity.User;
import com.mr.domain.user.exception.UserErrorStatus;
import com.mr.domain.user.repository.UserRepository;
import com.mr.global.apipayload.exception.GeneralException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StatisticsServiceTest {

    private static final ZoneId SERVICE_ZONE_ID = ZoneId.of("Asia/Seoul");
    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-07-26T15:30:00Z"), SERVICE_ZONE_ID);

    @Mock
    private UserRepository userRepository;

    @Mock
    private PlayingRepository playingRepository;

    @Mock
    private AnalysisRepository analysisRepository;

    private StatisticsService statisticsService;

    private final LocalDateTime thisWeekStart = LocalDate.now(FIXED_CLOCK).with(DayOfWeek.MONDAY).atStartOfDay();

    @BeforeEach
    void setUp() {
        statisticsService = new StatisticsService(userRepository, playingRepository, analysisRepository, FIXED_CLOCK);
    }

    private void stubBaseline(Long userId) {
        User user = mock(User.class);
        lenient().when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        lenient().when(playingRepository.findByUserAndStatusSince(anyLong(), any(), any())).thenReturn(List.of());
        lenient().when(analysisRepository.findByUserAndStatusSince(anyLong(), any(), any())).thenReturn(List.of());
    }

    private Playing mockPlaying(LocalDateTime endedAt, Integer durationSec) {
        Playing playing = mock(Playing.class);
        lenient().when(playing.getEndedAt()).thenReturn(endedAt);
        lenient().when(playing.getDurationSec()).thenReturn(durationSec);
        return playing;
    }

    private Analysis mockAnalysis(LocalDateTime completedAt, Integer totalScore, BigDecimal scaleScore,
            BigDecimal tensionScore, BigDecimal progressionScore, BigDecimal voiceLeadingScore) {
        Analysis analysis = mock(Analysis.class);
        lenient().when(analysis.getCompletedAt()).thenReturn(completedAt);
        lenient().when(analysis.getTotalScore()).thenReturn(totalScore);
        lenient().when(analysis.getScaleScore()).thenReturn(scaleScore);
        lenient().when(analysis.getTensionScore()).thenReturn(tensionScore);
        lenient().when(analysis.getProgressionScore()).thenReturn(progressionScore);
        lenient().when(analysis.getVoiceLeadingScore()).thenReturn(voiceLeadingScore);
        return analysis;
    }

    private DomainGrowth findDomain(StatisticsResponseDTO response, SkillType skillType) {
        return response.domainGrowth().stream()
                .filter(domain -> domain.domain() == skillType)
                .findFirst()
                .orElseThrow();
    }

    @Test
    @DisplayName("getStatistics - 존재하지 않는 사용자면 404")
    void getStatistics_userNotFound_throws404() {
        given(userRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> statisticsService.getStatistics(1L))
                .isInstanceOf(GeneralException.class)
                .hasFieldOrPropertyWithValue("code", UserErrorStatus.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("getStatistics - UTC 일요일이어도 KST 월요일 기준으로 조회 범위를 계산한다")
    void getStatistics_usesKstWeekBoundary() {
        stubBaseline(1L);

        statisticsService.getStatistics(1L);

        verify(playingRepository).findByUserAndStatusSince(
                1L, PlayingStatus.COMPLETED, LocalDateTime.of(2026, 7, 20, 0, 0));
        verify(analysisRepository).findByUserAndStatusSince(
                1L, AnalysisStatus.COMPLETED, LocalDateTime.of(2026, 7, 6, 0, 0));
    }
    @Test
    @DisplayName("getStatistics - 연습/분석 이력이 전혀 없으면 모든 수치가 0이다")
    void getStatistics_noData_allZero() {
        stubBaseline(1L);

        StatisticsResponseDTO response = statisticsService.getStatistics(1L);

        assertThat(response.weeklySummary().accuracy()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.weeklySummary().accuracy().scale()).isEqualTo(1);
        assertThat(response.weeklySummary().practiceMinutes()).isZero();
        assertThat(response.weeklySummary().completedSessionCount()).isZero();
        assertThat(response.weeklySummary().accuracyDiff()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.weeklySummary().accuracyDiff().scale()).isEqualTo(1);
        assertThat(response.weeklySummary().practiceMinutesDiff()).isZero();
        assertThat(response.weeklySummary().completedSessionCountDiff()).isZero();
        assertThat(response.domainGrowth()).allSatisfy(domain -> {
            assertThat(domain.currentScore()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(domain.previousScore()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(domain.diff()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(domain.currentScore().scale()).isEqualTo(1);
            assertThat(domain.previousScore().scale()).isEqualTo(1);
            assertThat(domain.diff().scale()).isEqualTo(1);
        });
        assertThat(response.weeklyTrend().diffFromPreviousWeek()).isZero();
        assertThat(response.weeklyTrend().items()).allSatisfy(
                item -> {
                    assertThat(item.averageScore()).isEqualByComparingTo(BigDecimal.ZERO);
                    assertThat(item.averageScore().scale()).isEqualTo(1);
                });
    }

    @Test
    @DisplayName("getStatistics - practiceMinutes/completedSessionCount는 Playing 기준으로 이번주/지난주를 분리 집계한다")
    void getStatistics_weeklySummary_countsFromPlayingOnly() {
        stubBaseline(1L);

        List<Playing> playings = List.of(
                mockPlaying(thisWeekStart.plusDays(1), 600),
                mockPlaying(thisWeekStart.plusDays(2), 600),
                mockPlaying(thisWeekStart.minusDays(3), 1200)
        );
        given(playingRepository.findByUserAndStatusSince(eq(1L), eq(PlayingStatus.COMPLETED), any()))
                .willReturn(playings);

        StatisticsResponseDTO response = statisticsService.getStatistics(1L);

        assertThat(response.weeklySummary().practiceMinutes()).isEqualTo(20);
        assertThat(response.weeklySummary().completedSessionCount()).isEqualTo(2);
        assertThat(response.weeklySummary().practiceMinutesDiff()).isEqualTo(0);
        assertThat(response.weeklySummary().completedSessionCountDiff()).isEqualTo(1);
    }

    @Test
    @DisplayName("getStatistics - 지난 주 연습 이력이 없어도 practiceMinutesDiff/completedSessionCountDiff는 0-가드 없이 단순 차감된다")
    void getStatistics_noPreviousWeekPlaying_diffIsPlainSubtraction() {
        stubBaseline(1L);
        List<Playing> playings = List.of(mockPlaying(thisWeekStart.plusDays(1), 600));
        given(playingRepository.findByUserAndStatusSince(eq(1L), eq(PlayingStatus.COMPLETED), any()))
                .willReturn(playings);

        StatisticsResponseDTO response = statisticsService.getStatistics(1L);

        assertThat(response.weeklySummary().practiceMinutesDiff()).isEqualTo(10);
        assertThat(response.weeklySummary().completedSessionCountDiff()).isEqualTo(1);
    }

    @Test
    @DisplayName("getStatistics - accuracy/domainGrowth는 Analysis totalScore/도메인 점수 평균과 이번주-지난주 diff를 계산한다")
    void getStatistics_accuracyAndDomainGrowth_computedFromAnalysis() {
        stubBaseline(1L);

        List<Analysis> analyses = List.of(
                mockAnalysis(thisWeekStart.plusDays(1), 90,
                        BigDecimal.valueOf(85), BigDecimal.valueOf(72), BigDecimal.valueOf(88), BigDecimal.valueOf(63)),
                mockAnalysis(thisWeekStart.minusDays(3), 80,
                        BigDecimal.valueOf(77), BigDecimal.valueOf(66), BigDecimal.valueOf(84), BigDecimal.valueOf(73))
        );
        given(analysisRepository.findByUserAndStatusSince(eq(1L), eq(AnalysisStatus.COMPLETED), any()))
                .willReturn(analyses);

        StatisticsResponseDTO response = statisticsService.getStatistics(1L);

        assertThat(response.weeklySummary().accuracy()).isEqualByComparingTo(BigDecimal.valueOf(90.0));
        assertThat(response.weeklySummary().accuracyDiff()).isEqualByComparingTo(BigDecimal.valueOf(10.0));

        DomainGrowth scale = findDomain(response, SkillType.SCALE);
        assertThat(scale.currentScore()).isEqualByComparingTo(BigDecimal.valueOf(85.0));
        assertThat(scale.previousScore()).isEqualByComparingTo(BigDecimal.valueOf(77.0));
        assertThat(scale.diff()).isEqualByComparingTo(BigDecimal.valueOf(8.0));

        DomainGrowth voiceLeading = findDomain(response, SkillType.VOICE_LEADING);
        assertThat(voiceLeading.diff()).isEqualByComparingTo(BigDecimal.valueOf(-10.0));
    }

    @Test
    @DisplayName("getStatistics - 지난 주 분석 이력이 없으면 accuracyDiff/domainGrowth diff는 0이다(0-가드)")
    void getStatistics_noPreviousWeekAnalysis_diffIsZero() {
        stubBaseline(1L);

        List<Analysis> analyses = List.of(
                mockAnalysis(thisWeekStart.plusDays(1), 90,
                        BigDecimal.valueOf(85), BigDecimal.valueOf(72), BigDecimal.valueOf(88), BigDecimal.valueOf(63))
        );
        given(analysisRepository.findByUserAndStatusSince(eq(1L), eq(AnalysisStatus.COMPLETED), any()))
                .willReturn(analyses);

        StatisticsResponseDTO response = statisticsService.getStatistics(1L);

        assertThat(response.weeklySummary().accuracy()).isEqualByComparingTo(BigDecimal.valueOf(90.0));
        assertThat(response.weeklySummary().accuracyDiff()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.weeklySummary().accuracyDiff().scale()).isEqualTo(1);
        assertThat(findDomain(response, SkillType.SCALE).diff()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("getStatistics - totalScore가 null인 Analysis는 평균 계산에서 제외된다")
    void getStatistics_nullTotalScore_excludedFromAverage() {
        stubBaseline(1L);

        List<Analysis> analyses = List.of(
                mockAnalysis(thisWeekStart.plusDays(1), 80, null, null, null, null),
                mockAnalysis(thisWeekStart.plusDays(1), null, null, null, null, null)
        );
        given(analysisRepository.findByUserAndStatusSince(eq(1L), eq(AnalysisStatus.COMPLETED), any()))
                .willReturn(analyses);

        StatisticsResponseDTO response = statisticsService.getStatistics(1L);

        assertThat(response.weeklySummary().accuracy()).isEqualByComparingTo(BigDecimal.valueOf(80.0));
    }

    @Test
    @DisplayName("getStatistics - completedSessionCount는 Playing 개수, accuracy는 Analysis 개수 기준으로 서로 다르게 집계된다")
    void getStatistics_playingVsAnalysisCount_areIndependent() {
        stubBaseline(1L);

        List<Playing> playings = List.of(mockPlaying(thisWeekStart.plusDays(1), 600));
        given(playingRepository.findByUserAndStatusSince(eq(1L), eq(PlayingStatus.COMPLETED), any()))
                .willReturn(playings);

        List<Analysis> analyses = List.of(
                mockAnalysis(thisWeekStart.plusDays(1), 80, null, null, null, null),
                mockAnalysis(thisWeekStart.plusDays(1), 90, null, null, null, null),
                mockAnalysis(thisWeekStart.plusDays(1), 100, null, null, null, null)
        );
        given(analysisRepository.findByUserAndStatusSince(eq(1L), eq(AnalysisStatus.COMPLETED), any()))
                .willReturn(analyses);

        StatisticsResponseDTO response = statisticsService.getStatistics(1L);

        assertThat(response.weeklySummary().completedSessionCount()).isEqualTo(1);
        assertThat(response.weeklySummary().accuracy()).isEqualByComparingTo(BigDecimal.valueOf(90.0));
    }

    @Test
    @DisplayName("getStatistics - weeklyTrend는 3주 전~이번주 순서로 4개이며 각 라벨이 고정된다")
    void getStatistics_weeklyTrend_fourItemsInFixedOrder() {
        stubBaseline(1L);

        List<Analysis> analyses = List.of(
                mockAnalysis(thisWeekStart.plusDays(1), 93, null, null, null, null),
                mockAnalysis(thisWeekStart.minusWeeks(1).plusDays(1), 63, null, null, null, null),
                mockAnalysis(thisWeekStart.minusWeeks(2).plusDays(1), 78, null, null, null, null),
                mockAnalysis(thisWeekStart.minusWeeks(3).plusDays(1), 60, null, null, null, null)
        );
        given(analysisRepository.findByUserAndStatusSince(eq(1L), eq(AnalysisStatus.COMPLETED), any()))
                .willReturn(analyses);

        StatisticsResponseDTO response = statisticsService.getStatistics(1L);

        List<TrendItem> items = response.weeklyTrend().items();
        assertThat(items).hasSize(4);
        assertThat(items.get(0).label()).isEqualTo("3주 전");
        assertThat(items.get(1).label()).isEqualTo("2주 전");
        assertThat(items.get(2).label()).isEqualTo("지난주");
        assertThat(items.get(3).label()).isEqualTo("이번주");
        assertThat(items.get(0).averageScore()).isEqualByComparingTo(BigDecimal.valueOf(60.0));
        assertThat(items.get(3).averageScore()).isEqualByComparingTo(BigDecimal.valueOf(93.0));
        assertThat(response.weeklyTrend().diffFromPreviousWeek()).isEqualTo(30);
    }

    @Test
    @DisplayName("getStatistics - 4주 전(범위 밖) 데이터는 weeklyTrend 집계에서 제외된다")
    void getStatistics_dataOlderThanFourWeeks_excludedFromTrend() {
        stubBaseline(1L);

        List<Analysis> analyses = List.of(
                mockAnalysis(thisWeekStart.minusWeeks(4).minusDays(1), 999, null, null, null, null)
        );
        given(analysisRepository.findByUserAndStatusSince(eq(1L), eq(AnalysisStatus.COMPLETED), any()))
                .willReturn(analyses);

        StatisticsResponseDTO response = statisticsService.getStatistics(1L);

        assertThat(response.weeklyTrend().items()).allSatisfy(
                item -> {
                    assertThat(item.averageScore()).isEqualByComparingTo(BigDecimal.ZERO);
                    assertThat(item.averageScore().scale()).isEqualTo(1);
                });
    }

}
