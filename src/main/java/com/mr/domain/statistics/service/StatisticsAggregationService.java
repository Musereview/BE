package com.mr.domain.statistics.service;

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
import com.mr.domain.user.exception.UserErrorStatus;
import com.mr.domain.user.repository.UserRepository;
import com.mr.global.apipayload.exception.GeneralException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

// 연습/분석 완료 이벤트를 받아 UserStatistics/PracticeStatistics/SkillStatistics를 다시 계산해 저장
@Service
@RequiredArgsConstructor
@Transactional
public class StatisticsAggregationService {

    private static final int SCORE_SCALE = 1;
    private static final int SECONDS_PER_MINUTE = 60;

    private final UserRepository userRepository;
    private final UserStatisticsRepository userStatisticsRepository;
    private final PracticeStatisticsRepository practiceStatisticsRepository;
    private final SkillStatisticsRepository skillStatisticsRepository;
    private final PlayingRepository playingRepository;
    private final AnalysisRepository analysisRepository;

    // 리스너의 @Retryable과 같은 메서드에 두면 advice 순서가 불명확해지므로,
    // 트랜잭션 경계는 반드시 이 별도 빈의 메서드에서 열어 재시도마다 새 트랜잭션이 보장되도록 함
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onPlayingCompleted(Long userId) {
        refreshUserPracticeTotals(userId);
        refreshWeeklyPracticeStatistics(userId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onAnalysisCompleted(Long userId) {
        refreshUserAnalysisTotals(userId);
        refreshWeeklyPracticeStatistics(userId);
        refreshWeeklySkillStatistics(userId);
    }

    private void refreshUserPracticeTotals(Long userId) {
        PlayingRepository.PracticeTotals totals =
                playingRepository.aggregateTotalsByUserAndStatus(userId, PlayingStatus.COMPLETED);

        UserStatistics stats = getOrCreateUserStatistics(userId);
        stats.updatePracticeSummary(
                minutesFromSeconds(totals.getTotalDurationSec()),
                totals.getSessionCount().intValue(),
                totals.getLastEndedAt());
    }

    private void refreshUserAnalysisTotals(Long userId) {
        AnalysisRepository.AnalysisTotals totals =
                analysisRepository.aggregateTotalsByUserAndStatus(userId, AnalysisStatus.COMPLETED);

        UserStatistics stats = getOrCreateUserStatistics(userId);
        stats.updateAnalysisSummary(
                totals.getAnalysisCount().intValue(),
                toScoreScale(totals.getAverageTotalScore()));
    }

    // 동시 이벤트로 인해 유니크 제약을 어겨 저장에 실패해도, 리스너의 재시도가 재조회 후 갱신으로 이어져 정상 처리됨
    private UserStatistics getOrCreateUserStatistics(Long userId) {
        return userStatisticsRepository.findByUser_UserId(userId)
                .orElseGet(() -> userStatisticsRepository.save(UserStatistics.createForUser(getUser(userId))));
    }

    private void refreshWeeklyPracticeStatistics(Long userId) {
        LocalDate weekStart = LocalDate.now().with(DayOfWeek.MONDAY);
        LocalDate weekEnd = weekStart.plusDays(6);

        List<Playing> playings = playingRepository.findByUserAndStatusSince(
                userId, PlayingStatus.COMPLETED, weekStart.atStartOfDay());
        List<Analysis> analyses = analysisRepository.findByUserAndStatusSince(
                userId, AnalysisStatus.COMPLETED, weekStart.atStartOfDay());

        int practiceMinutes = minutesFromSeconds(sumDurationSec(playings));
        int sessionCount = playings.size();
        BigDecimal averageAccuracy = averageTotalScore(analyses);

        PracticeStatistics stats = practiceStatisticsRepository
                .findByUser_UserIdAndPeriodTypeAndPeriodStart(userId, PeriodType.WEEKLY, weekStart)
                .orElseGet(() -> practiceStatisticsRepository.save(
                        PracticeStatistics.create(getUser(userId), PeriodType.WEEKLY, weekStart, weekEnd)));

        stats.update(practiceMinutes, sessionCount, averageAccuracy);
    }

    private void refreshWeeklySkillStatistics(Long userId) {
        LocalDate weekStart = LocalDate.now().with(DayOfWeek.MONDAY);
        LocalDate weekEnd = weekStart.plusDays(6);
        LocalDate lastWeekStart = weekStart.minusWeeks(1);

        List<Analysis> analyses = analysisRepository.findByUserAndStatusSince(
                userId, AnalysisStatus.COMPLETED, weekStart.atStartOfDay());

        for (SkillType skillType : SkillType.values()) {
            upsertWeeklySkillStatistics(userId, skillType, weekStart, weekEnd, lastWeekStart, analyses);
        }
    }

    private void upsertWeeklySkillStatistics(Long userId, SkillType skillType, LocalDate weekStart,
            LocalDate weekEnd, LocalDate lastWeekStart, List<Analysis> analyses) {
        BigDecimal score = averageSkillScore(analyses, skillType);
        if (score == null) {
            // score는 NOT NULL 컬럼 - 이번 주 유효한 점수가 없으면 생성/갱신하지 않고 건너뜀
            return;
        }

        skillStatisticsRepository
                .findByUser_UserIdAndPeriodTypeAndPeriodStartAndSkillType(userId, PeriodType.WEEKLY, weekStart, skillType)
                .ifPresentOrElse(
                        existing -> existing.updateScore(score, existing.getPreviousScore()),
                        () -> {
                            BigDecimal previousScore = skillStatisticsRepository
                                    .findByUser_UserIdAndPeriodTypeAndPeriodStartAndSkillType(
                                            userId, PeriodType.WEEKLY, lastWeekStart, skillType)
                                    .map(SkillStatistics::getScore)
                                    .orElse(null);

                            skillStatisticsRepository.save(SkillStatistics.createWithPreviousScore(
                                    getUser(userId), PeriodType.WEEKLY, weekStart, weekEnd, skillType, score, previousScore));
                        }
                );
    }

    private long sumDurationSec(List<Playing> playings) {
        return playings.stream()
                .mapToLong(p -> p.getDurationSec() != null ? p.getDurationSec() : 0)
                .sum();
    }

    private BigDecimal averageTotalScore(List<Analysis> analyses) {
        List<Integer> scores = analyses.stream()
                .map(Analysis::getTotalScore)
                .filter(Objects::nonNull)
                .toList();
        return average(scores.stream().map(BigDecimal::valueOf).toList());
    }

    private BigDecimal averageSkillScore(List<Analysis> analyses, SkillType skillType) {
        List<BigDecimal> scores = analyses.stream()
                .map(a -> AnalysisSkillScoreResolver.resolve(a, skillType))
                .filter(Objects::nonNull)
                .toList();
        return average(scores);
    }

    private BigDecimal average(List<BigDecimal> scores) {
        if (scores.isEmpty()) {
            return null;
        }
        BigDecimal sum = scores.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(scores.size()), SCORE_SCALE, RoundingMode.HALF_UP);
    }

    private int minutesFromSeconds(Long totalSeconds) {
        return (int) ((totalSeconds != null ? totalSeconds : 0L) / SECONDS_PER_MINUTE);
    }

    private BigDecimal toScoreScale(Double average) {
        if (average == null) {
            return null;
        }
        return BigDecimal.valueOf(average).setScale(SCORE_SCALE, RoundingMode.HALF_UP);
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorStatus.USER_NOT_FOUND));
    }
}
