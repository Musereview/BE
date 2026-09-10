package com.mr.domain.statistics.service;

import com.mr.domain.analysis.entity.enums.AnalysisStatus;
import com.mr.domain.analysis.repository.AnalysisRepository;
import com.mr.domain.statistics.entity.SkillStatistics;
import com.mr.domain.statistics.entity.enums.PeriodType;
import com.mr.domain.statistics.entity.enums.SkillType;
import com.mr.domain.statistics.repository.SkillStatisticsRepository;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WeeklySkillStatisticsAggregationService {

    private static final int SCORE_SCALE = 1;
    private static final ZoneId KOREA_ZONE_ID = ZoneId.of("Asia/Seoul");

    private final UserRepository userRepository;
    private final SkillStatisticsRepository skillStatisticsRepository;
    private final AnalysisRepository analysisRepository;
    private final Clock clock;

    public void aggregate(Long userId) {
        LocalDate weekStart = Instant.now(clock).atZone(KOREA_ZONE_ID).toLocalDate().with(DayOfWeek.MONDAY);
        LocalDate weekEnd = weekStart.plusDays(6);
        LocalDate lastWeekStart = weekStart.minusWeeks(1);

        AnalysisRepository.WeeklySkillAverages averages = analysisRepository.aggregateWeeklySkillAveragesByUserAndStatusSince(
                userId, AnalysisStatus.COMPLETED, weekStart.atStartOfDay(KOREA_ZONE_ID).toInstant());

        for (SkillType skillType : SkillType.values()) {
            upsert(userId, skillType, weekStart, weekEnd, lastWeekStart, averages);
        }
    }

    private void upsert(Long userId, SkillType skillType, LocalDate weekStart,
            LocalDate weekEnd, LocalDate lastWeekStart, AnalysisRepository.WeeklySkillAverages averages) {
        BigDecimal score = resolveScore(averages, skillType);
        if (score == null) {
            return;
        }

        skillStatisticsRepository
                .findByUser_UserIdAndPeriodTypeAndPeriodStartAndSkillType(userId, PeriodType.WEEKLY, weekStart, skillType)
                .ifPresentOrElse(
                        existing -> existing.updateScore(score, existing.getPreviousScore()),
                        () -> create(userId, skillType, weekStart, weekEnd, lastWeekStart, score));
    }

    private void create(Long userId, SkillType skillType, LocalDate weekStart,
            LocalDate weekEnd, LocalDate lastWeekStart, BigDecimal score) {
        BigDecimal previousScore = skillStatisticsRepository
                .findByUser_UserIdAndPeriodTypeAndPeriodStartAndSkillType(
                        userId, PeriodType.WEEKLY, lastWeekStart, skillType)
                .map(SkillStatistics::getScore)
                .orElse(null);

        skillStatisticsRepository.save(SkillStatistics.createWithPreviousScore(
                getUser(userId), PeriodType.WEEKLY, weekStart, weekEnd, skillType, score, previousScore));
    }

    private BigDecimal resolveScore(AnalysisRepository.WeeklySkillAverages averages, SkillType skillType) {
        Double average = switch (skillType) {
            case SCALE -> averages.getScaleScore();
            case TENSION -> averages.getTensionScore();
            case PROGRESSION -> averages.getProgressionScore();
            case VOICE_LEADING -> averages.getVoiceLeadingScore();
        };
        return average == null
                ? null
                : BigDecimal.valueOf(average).setScale(SCORE_SCALE, RoundingMode.HALF_UP);
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorStatus.USER_NOT_FOUND));
    }
}
