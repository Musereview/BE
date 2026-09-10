package com.mr.domain.statistics.service;

import com.mr.domain.analysis.entity.Analysis;
import com.mr.domain.analysis.entity.enums.AnalysisStatus;
import com.mr.domain.analysis.repository.AnalysisRepository;
import com.mr.domain.playing.entity.enums.PlayingStatus;
import com.mr.domain.playing.repository.PlayingRepository;
import com.mr.domain.statistics.entity.PracticeStatistics;
import com.mr.domain.statistics.entity.enums.PeriodType;
import com.mr.domain.statistics.repository.PracticeStatisticsRepository;
import com.mr.domain.user.entity.User;
import com.mr.domain.user.exception.UserErrorStatus;
import com.mr.domain.user.repository.UserRepository;
import com.mr.global.apipayload.exception.GeneralException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WeeklyPracticeStatisticsAggregationService {

    private static final int SCORE_SCALE = 1;
    private static final int SECONDS_PER_MINUTE = 60;
    private static final ZoneId KOREA_ZONE_ID = ZoneId.of("Asia/Seoul");

    private final UserRepository userRepository;
    private final PracticeStatisticsRepository practiceStatisticsRepository;
    private final PlayingRepository playingRepository;
    private final AnalysisRepository analysisRepository;
    private final Clock clock;

    public void aggregate(Long userId) {
        LocalDate weekStart = Instant.now(clock).atZone(KOREA_ZONE_ID).toLocalDate().with(DayOfWeek.MONDAY);
        LocalDate weekEnd = weekStart.plusDays(6);
        Instant aggregationStart = weekStart.atStartOfDay(KOREA_ZONE_ID).toInstant();

        PlayingRepository.WeeklyPracticeTotals practiceTotals = playingRepository.aggregateTotalsByUserAndStatusSince(
                userId, PlayingStatus.COMPLETED, aggregationStart);
        List<Analysis> analyses = analysisRepository.findByUserAndStatusSince(
                userId, AnalysisStatus.COMPLETED, aggregationStart);

        PracticeStatistics statistics = practiceStatisticsRepository
                .findByUser_UserIdAndPeriodTypeAndPeriodStart(userId, PeriodType.WEEKLY, weekStart)
                .orElseGet(() -> practiceStatisticsRepository.save(
                        PracticeStatistics.create(getUser(userId), PeriodType.WEEKLY, weekStart, weekEnd)));
        statistics.update(
                minutesFromSeconds(practiceTotals.getTotalDurationSec()),
                practiceTotals.getSessionCount().intValue(),
                averageTotalScore(analyses));
    }

    private BigDecimal averageTotalScore(List<Analysis> analyses) {
        List<BigDecimal> scores = analyses.stream()
                .map(Analysis::getTotalScore)
                .filter(Objects::nonNull)
                .map(BigDecimal::valueOf)
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

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorStatus.USER_NOT_FOUND));
    }
}
