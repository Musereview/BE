package com.mr.domain.statistics.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.mr.domain.analysis.entity.Analysis;
import com.mr.domain.analysis.entity.enums.AnalysisStatus;
import com.mr.domain.analysis.repository.AnalysisRepository;
import com.mr.domain.playing.entity.Playing;
import com.mr.domain.playing.entity.enums.PlayingStatus;
import com.mr.domain.playing.repository.PlayingRepository;
import com.mr.domain.statistics.entity.PracticeStatistics;
import com.mr.domain.statistics.entity.SkillStatistics;
import com.mr.domain.statistics.entity.UserStatistics;
import com.mr.domain.statistics.entity.enums.PeriodType;
import com.mr.domain.statistics.entity.enums.SkillType;
import com.mr.domain.statistics.repository.PracticeStatisticsRepository;
import com.mr.domain.statistics.repository.SkillStatisticsRepository;
import com.mr.domain.statistics.repository.UserStatisticsRepository;
import com.mr.domain.user.entity.User;
import com.mr.domain.user.repository.UserRepository;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class StatisticsAggregationServiceTest {

    private static final ZoneId SERVICE_ZONE_ID = ZoneId.of("Asia/Seoul");
    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-07-26T15:30:00Z"), SERVICE_ZONE_ID);

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserStatisticsRepository userStatisticsRepository;
    @Mock
    private PracticeStatisticsRepository practiceStatisticsRepository;
    @Mock
    private SkillStatisticsRepository skillStatisticsRepository;
    @Mock
    private PlayingRepository playingRepository;
    @Mock
    private AnalysisRepository analysisRepository;

    private StatisticsAggregationService service;

    private final Long userId = 1L;
    private final LocalDate weekStart = LocalDate.now(FIXED_CLOCK).with(DayOfWeek.MONDAY);
    private final LocalDate weekEnd = weekStart.plusDays(6);
    private final LocalDate lastWeekStart = weekStart.minusWeeks(1);

    @BeforeEach
    void setUp() {
        service = new StatisticsAggregationService(
                userRepository, userStatisticsRepository, practiceStatisticsRepository,
                skillStatisticsRepository, playingRepository, analysisRepository, FIXED_CLOCK);
    }

    private void stubBaseline() {
        User user = mock(User.class);
        lenient().when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        lenient().when(playingRepository.findByUserAndStatusSince(anyLong(), any(), any())).thenReturn(List.of());
        lenient().when(analysisRepository.findByUserAndStatusSince(anyLong(), any(), any())).thenReturn(List.of());
        lenient().when(practiceStatisticsRepository
                        .findByUser_UserIdAndPeriodTypeAndPeriodStart(anyLong(), any(), any()))
                .thenReturn(Optional.empty());
        lenient().when(practiceStatisticsRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(skillStatisticsRepository
                        .findByUser_UserIdAndPeriodTypeAndPeriodStartAndSkillType(anyLong(), any(), any(), any()))
                .thenReturn(Optional.empty());
        lenient().when(skillStatisticsRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private PlayingRepository.PracticeTotals mockPracticeTotals(
            long sessionCount, long totalDurationSec, LocalDateTime lastEndedAt) {
        PlayingRepository.PracticeTotals totals = mock(PlayingRepository.PracticeTotals.class);
        lenient().when(totals.getSessionCount()).thenReturn(sessionCount);
        lenient().when(totals.getTotalDurationSec()).thenReturn(totalDurationSec);
        lenient().when(totals.getLastEndedAt()).thenReturn(lastEndedAt);
        return totals;
    }

    private AnalysisRepository.AnalysisTotals mockAnalysisTotals(long analysisCount, Double averageTotalScore) {
        AnalysisRepository.AnalysisTotals totals = mock(AnalysisRepository.AnalysisTotals.class);
        lenient().when(totals.getAnalysisCount()).thenReturn(analysisCount);
        lenient().when(totals.getAverageTotalScore()).thenReturn(averageTotalScore);
        return totals;
    }

    private Playing mockPlaying(Integer durationSec) {
        Playing playing = mock(Playing.class);
        lenient().when(playing.getDurationSec()).thenReturn(durationSec);
        return playing;
    }

    private Analysis mockAnalysis(Integer totalScore, BigDecimal scaleScore, BigDecimal tensionScore,
            BigDecimal progressionScore, BigDecimal voiceLeadingScore) {
        Analysis analysis = mock(Analysis.class);
        lenient().when(analysis.getTotalScore()).thenReturn(totalScore);
        lenient().when(analysis.getScaleScore()).thenReturn(scaleScore);
        lenient().when(analysis.getTensionScore()).thenReturn(tensionScore);
        lenient().when(analysis.getProgressionScore()).thenReturn(progressionScore);
        lenient().when(analysis.getVoiceLeadingScore()).thenReturn(voiceLeadingScore);
        return analysis;
    }

    private UserStatistics captureSavedUserStatistics() {
        ArgumentCaptor<UserStatistics> captor = ArgumentCaptor.forClass(UserStatistics.class);
        verify(userStatisticsRepository).save(captor.capture());
        return captor.getValue();
    }

    private PracticeStatistics captureSavedPracticeStatistics() {
        ArgumentCaptor<PracticeStatistics> captor = ArgumentCaptor.forClass(PracticeStatistics.class);
        verify(practiceStatisticsRepository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("onPlayingCompleted - UserStatistics가 없으면 새로 만들어 전체 기간 집계값을 채운다")
    void onPlayingCompleted_noExistingUserStatistics_createsWithAggregatedTotals() {
        stubBaseline();
        LocalDateTime lastEndedAt = LocalDateTime.now();
        PlayingRepository.PracticeTotals totals = mockPracticeTotals(5L, 1200L, lastEndedAt);
        given(playingRepository.aggregateTotalsByUserAndStatus(userId, PlayingStatus.COMPLETED))
                .willReturn(totals);
        given(userStatisticsRepository.findByUser_UserId(userId)).willReturn(Optional.empty());
        given(userStatisticsRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        service.onPlayingCompleted(userId);

        UserStatistics saved = captureSavedUserStatistics();
        assertThat(saved.getTotalPracticeMinutes()).isEqualTo(20);
        assertThat(saved.getTotalPracticeCount()).isEqualTo(5);
        assertThat(saved.getRecentPracticedAt()).isEqualTo(lastEndedAt);
    }

    @Test
    @DisplayName("onPlayingCompleted - UserStatistics가 이미 있으면 새로 저장하지 않고 값만 갱신한다")
    void onPlayingCompleted_existingUserStatistics_updatesWithoutCreating() {
        stubBaseline();
        UserStatistics existing = UserStatistics.createForUser(mock(User.class));
        PlayingRepository.PracticeTotals totals = mockPracticeTotals(3L, 600L, LocalDateTime.now());
        given(playingRepository.aggregateTotalsByUserAndStatus(userId, PlayingStatus.COMPLETED))
                .willReturn(totals);
        given(userStatisticsRepository.findByUser_UserId(userId)).willReturn(Optional.of(existing));

        service.onPlayingCompleted(userId);

        assertThat(existing.getTotalPracticeMinutes()).isEqualTo(10);
        assertThat(existing.getTotalPracticeCount()).isEqualTo(3);
        verify(userStatisticsRepository, never()).save(any());
    }

    @Test
    @DisplayName("onAnalysisCompleted - averageAccuracy는 COMPLETED Analysis의 totalScore 평균이고 averageScore는 채우지 않는다")
    void onAnalysisCompleted_fillsAverageAccuracyOnly() {
        stubBaseline();
        AnalysisRepository.AnalysisTotals totals = mockAnalysisTotals(4L, 82.5);
        given(analysisRepository.aggregateTotalsByUserAndStatus(userId, AnalysisStatus.COMPLETED))
                .willReturn(totals);
        given(userStatisticsRepository.findByUser_UserId(userId)).willReturn(Optional.empty());
        given(userStatisticsRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        service.onAnalysisCompleted(userId);

        UserStatistics saved = captureSavedUserStatistics();
        assertThat(saved.getTotalAnalysisCount()).isEqualTo(4);
        assertThat(saved.getAverageAccuracy()).isEqualByComparingTo("82.5");
        assertThat(saved.getAverageScore()).isNull();
    }

    @Test
    @DisplayName("onPlayingCompleted - 이번 주 PracticeStatistics가 없으면 새로 만들어 연습 시간/횟수/정확도를 채운다")
    void onPlayingCompleted_noWeeklyPracticeStatistics_createsWithWeeklyAggregates() {
        stubBaseline();
        PlayingRepository.PracticeTotals emptyPracticeTotals = mockPracticeTotals(0L, 0L, null);
        given(playingRepository.aggregateTotalsByUserAndStatus(userId, PlayingStatus.COMPLETED))
                .willReturn(emptyPracticeTotals);
        given(userStatisticsRepository.findByUser_UserId(userId))
                .willReturn(Optional.of(UserStatistics.createForUser(mock(User.class))));
        List<Playing> weeklyPlayings = List.of(mockPlaying(600), mockPlaying(1200));
        given(playingRepository.findByUserAndStatusSince(userId, PlayingStatus.COMPLETED, weekStart.atStartOfDay()))
                .willReturn(weeklyPlayings);
        List<Analysis> weeklyAnalyses = List.of(
                mockAnalysis(90, null, null, null, null), mockAnalysis(80, null, null, null, null));
        given(analysisRepository.findByUserAndStatusSince(userId, AnalysisStatus.COMPLETED, weekStart.atStartOfDay()))
                .willReturn(weeklyAnalyses);

        service.onPlayingCompleted(userId);

        PracticeStatistics saved = captureSavedPracticeStatistics();
        assertThat(saved.getPeriodType()).isEqualTo(PeriodType.WEEKLY);
        assertThat(saved.getPeriodStart()).isEqualTo(weekStart);
        assertThat(saved.getPeriodEnd()).isEqualTo(weekEnd);
        assertThat(saved.getPracticeMinutes()).isEqualTo(30);
        assertThat(saved.getSessionCount()).isEqualTo(2);
        assertThat(saved.getAverageAccuracy()).isEqualByComparingTo("85.0");
    }

    @Test
    @DisplayName("onPlayingCompleted - 이번 주에 완료된 연습/분석이 없으면 평균 정확도는 null이다")
    void onPlayingCompleted_noDataThisWeek_averageAccuracyIsNull() {
        stubBaseline();
        PlayingRepository.PracticeTotals emptyPracticeTotals = mockPracticeTotals(0L, 0L, null);
        given(playingRepository.aggregateTotalsByUserAndStatus(userId, PlayingStatus.COMPLETED))
                .willReturn(emptyPracticeTotals);
        given(userStatisticsRepository.findByUser_UserId(userId))
                .willReturn(Optional.of(UserStatistics.createForUser(mock(User.class))));

        service.onPlayingCompleted(userId);

        PracticeStatistics saved = captureSavedPracticeStatistics();
        assertThat(saved.getPracticeMinutes()).isZero();
        assertThat(saved.getSessionCount()).isZero();
        assertThat(saved.getAverageAccuracy()).isNull();
    }

    @Test
    @DisplayName("onPlayingCompleted - 이번 주 PracticeStatistics가 이미 있으면 새로 저장하지 않고 값만 갱신한다")
    void onPlayingCompleted_existingWeeklyPracticeStatistics_updatesWithoutCreating() {
        stubBaseline();
        PlayingRepository.PracticeTotals emptyPracticeTotals = mockPracticeTotals(0L, 0L, null);
        given(playingRepository.aggregateTotalsByUserAndStatus(userId, PlayingStatus.COMPLETED))
                .willReturn(emptyPracticeTotals);
        given(userStatisticsRepository.findByUser_UserId(userId))
                .willReturn(Optional.of(UserStatistics.createForUser(mock(User.class))));

        PracticeStatistics existing = PracticeStatistics.create(mock(User.class), PeriodType.WEEKLY, weekStart, weekEnd);
        given(practiceStatisticsRepository.findByUser_UserIdAndPeriodTypeAndPeriodStart(userId, PeriodType.WEEKLY, weekStart))
                .willReturn(Optional.of(existing));
        List<Playing> weeklyPlayings = List.of(mockPlaying(300));
        given(playingRepository.findByUserAndStatusSince(userId, PlayingStatus.COMPLETED, weekStart.atStartOfDay()))
                .willReturn(weeklyPlayings);

        service.onPlayingCompleted(userId);

        assertThat(existing.getPracticeMinutes()).isEqualTo(5);
        assertThat(existing.getSessionCount()).isEqualTo(1);
        verify(practiceStatisticsRepository, never()).save(any());
    }

    @Test
    @DisplayName("onAnalysisCompleted - 이번 주 SkillStatistics가 없으면 지난주 점수를 previousScore로 채워 새로 만든다")
    void onAnalysisCompleted_noCurrentWeekSkillStatistics_createsWithLastWeekAsPrevious() {
        stubBaseline();
        AnalysisRepository.AnalysisTotals emptyAnalysisTotals = mockAnalysisTotals(0L, null);
        given(analysisRepository.aggregateTotalsByUserAndStatus(userId, AnalysisStatus.COMPLETED))
                .willReturn(emptyAnalysisTotals);
        given(userStatisticsRepository.findByUser_UserId(userId))
                .willReturn(Optional.of(UserStatistics.createForUser(mock(User.class))));
        List<Analysis> weeklyAnalyses = List.of(mockAnalysis(null,
                new BigDecimal("90.0"), new BigDecimal("70.0"), new BigDecimal("88.0"), new BigDecimal("60.0")));
        given(analysisRepository.findByUserAndStatusSince(userId, AnalysisStatus.COMPLETED, weekStart.atStartOfDay()))
                .willReturn(weeklyAnalyses);

        SkillStatistics lastWeekScale = SkillStatistics.create(
                mock(User.class), PeriodType.WEEKLY, lastWeekStart, weekStart.minusDays(1),
                SkillType.SCALE, new BigDecimal("77.0"));
        given(skillStatisticsRepository.findByUser_UserIdAndPeriodTypeAndPeriodStartAndSkillType(
                userId, PeriodType.WEEKLY, lastWeekStart, SkillType.SCALE))
                .willReturn(Optional.of(lastWeekScale));

        service.onAnalysisCompleted(userId);

        ArgumentCaptor<SkillStatistics> captor = ArgumentCaptor.forClass(SkillStatistics.class);
        verify(skillStatisticsRepository, atLeast(4)).save(captor.capture());

        SkillStatistics savedScale = captor.getAllValues().stream()
                .filter(s -> s.getSkillType() == SkillType.SCALE)
                .findFirst().orElseThrow();
        assertThat(savedScale.getScore()).isEqualByComparingTo("90.0");
        assertThat(savedScale.getPreviousScore()).isEqualByComparingTo("77.0");

        SkillStatistics savedTension = captor.getAllValues().stream()
                .filter(s -> s.getSkillType() == SkillType.TENSION)
                .findFirst().orElseThrow();
        assertThat(savedTension.getScore()).isEqualByComparingTo("70.0");
        assertThat(savedTension.getPreviousScore()).isNull();

        SkillStatistics savedProgression = captor.getAllValues().stream()
                .filter(s -> s.getSkillType() == SkillType.PROGRESSION)
                .findFirst().orElseThrow();
        assertThat(savedProgression.getScore()).isEqualByComparingTo("88.0");

        SkillStatistics savedVoiceLeading = captor.getAllValues().stream()
                .filter(s -> s.getSkillType() == SkillType.VOICE_LEADING)
                .findFirst().orElseThrow();
        assertThat(savedVoiceLeading.getScore()).isEqualByComparingTo("60.0");
    }

    @Test
    @DisplayName("onAnalysisCompleted - 이번 주 SkillStatistics가 이미 있으면 previousScore는 그대로 두고 score만 갱신한다")
    void onAnalysisCompleted_existingCurrentWeekSkillStatistics_keepsPreviousScore() {
        stubBaseline();
        AnalysisRepository.AnalysisTotals emptyAnalysisTotals = mockAnalysisTotals(0L, null);
        given(analysisRepository.aggregateTotalsByUserAndStatus(userId, AnalysisStatus.COMPLETED))
                .willReturn(emptyAnalysisTotals);
        given(userStatisticsRepository.findByUser_UserId(userId))
                .willReturn(Optional.of(UserStatistics.createForUser(mock(User.class))));
        List<Analysis> weeklyAnalyses = List.of(mockAnalysis(null,
                new BigDecimal("90.0"), new BigDecimal("91.0"), new BigDecimal("92.0"), new BigDecimal("93.0")));
        given(analysisRepository.findByUserAndStatusSince(userId, AnalysisStatus.COMPLETED, weekStart.atStartOfDay()))
                .willReturn(weeklyAnalyses);

        SkillStatistics existingScale = SkillStatistics.createWithPreviousScore(
                mock(User.class), PeriodType.WEEKLY, weekStart, weekEnd, SkillType.SCALE,
                new BigDecimal("50.0"), new BigDecimal("40.0"));
        given(skillStatisticsRepository.findByUser_UserIdAndPeriodTypeAndPeriodStartAndSkillType(
                userId, PeriodType.WEEKLY, weekStart, SkillType.SCALE))
                .willReturn(Optional.of(existingScale));

        SkillStatistics existingTension = SkillStatistics.createWithPreviousScore(
                mock(User.class), PeriodType.WEEKLY, weekStart, weekEnd, SkillType.TENSION,
                new BigDecimal("55.0"), new BigDecimal("45.0"));
        given(skillStatisticsRepository.findByUser_UserIdAndPeriodTypeAndPeriodStartAndSkillType(
                userId, PeriodType.WEEKLY, weekStart, SkillType.TENSION))
                .willReturn(Optional.of(existingTension));

        service.onAnalysisCompleted(userId);

        assertThat(existingScale.getScore()).isEqualByComparingTo("90.0");
        assertThat(existingScale.getPreviousScore()).isEqualByComparingTo("40.0");
        assertThat(existingTension.getScore()).isEqualByComparingTo("91.0");
        assertThat(existingTension.getPreviousScore()).isEqualByComparingTo("45.0");

        ArgumentCaptor<SkillStatistics> captor = ArgumentCaptor.forClass(SkillStatistics.class);
        verify(skillStatisticsRepository, atLeast(2)).save(captor.capture());

        SkillStatistics savedProgression = captor.getAllValues().stream()
                .filter(s -> s.getSkillType() == SkillType.PROGRESSION)
                .findFirst().orElseThrow();
        assertThat(savedProgression.getScore()).isEqualByComparingTo("92.0");

        SkillStatistics savedVoiceLeading = captor.getAllValues().stream()
                .filter(s -> s.getSkillType() == SkillType.VOICE_LEADING)
                .findFirst().orElseThrow();
        assertThat(savedVoiceLeading.getScore()).isEqualByComparingTo("93.0");
    }

    @Test
    @DisplayName("onAnalysisCompleted - 이번 주 특정 스킬 점수가 전부 null이면 해당 스킬은 건드리지 않는다")
    void onAnalysisCompleted_skillScoreAllNull_skipsThatSkillType() {
        stubBaseline();
        AnalysisRepository.AnalysisTotals emptyAnalysisTotals = mockAnalysisTotals(0L, null);
        given(analysisRepository.aggregateTotalsByUserAndStatus(userId, AnalysisStatus.COMPLETED))
                .willReturn(emptyAnalysisTotals);
        given(userStatisticsRepository.findByUser_UserId(userId))
                .willReturn(Optional.of(UserStatistics.createForUser(mock(User.class))));
        List<Analysis> weeklyAnalyses = List.of(mockAnalysis(null,
                null, new BigDecimal("70.0"), new BigDecimal("80.0"), new BigDecimal("90.0")));
        given(analysisRepository.findByUserAndStatusSince(userId, AnalysisStatus.COMPLETED, weekStart.atStartOfDay()))
                .willReturn(weeklyAnalyses);

        service.onAnalysisCompleted(userId);

        verify(skillStatisticsRepository, never())
                .save(argThat(s -> s.getSkillType() == SkillType.SCALE));
        ArgumentCaptor<SkillStatistics> captor = ArgumentCaptor.forClass(SkillStatistics.class);
        verify(skillStatisticsRepository, atLeast(3)).save(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(SkillStatistics::getSkillType)
                .doesNotContain(SkillType.SCALE);
    }

    @Test
    @DisplayName("onPlayingCompleted - UserStatistics 저장 중 예외가 발생하면 삼키지 않고 그대로 전파한다")
    void onPlayingCompleted_saveFails_propagatesException() {
        stubBaseline();
        PlayingRepository.PracticeTotals totals = mockPracticeTotals(1L, 60L, LocalDateTime.now());
        given(playingRepository.aggregateTotalsByUserAndStatus(userId, PlayingStatus.COMPLETED))
                .willReturn(totals);
        given(userStatisticsRepository.findByUser_UserId(userId)).willReturn(Optional.empty());
        given(userStatisticsRepository.save(any()))
                .willThrow(new DataIntegrityViolationException("unique violation"));

        assertThatThrownBy(() -> service.onPlayingCompleted(userId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
