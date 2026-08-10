package com.mr.domain.statistics.service;

import com.mr.domain.statistics.dto.res.StatisticsResponseDTO;
import com.mr.domain.statistics.dto.res.StatisticsResponseDTO.DomainGrowth;
import com.mr.domain.statistics.dto.res.StatisticsResponseDTO.TrendItem;
import com.mr.domain.statistics.dto.res.StatisticsResponseDTO.WeeklySummary;
import com.mr.domain.statistics.dto.res.StatisticsResponseDTO.WeeklyTrend;
import com.mr.domain.statistics.entity.PracticeStatistics;
import com.mr.domain.statistics.entity.SkillStatistics;
import com.mr.domain.statistics.entity.enums.PeriodType;
import com.mr.domain.statistics.entity.enums.SkillType;
import com.mr.domain.statistics.repository.PracticeStatisticsRepository;
import com.mr.domain.statistics.repository.SkillStatisticsRepository;
import com.mr.domain.user.exception.UserErrorStatus;
import com.mr.domain.user.repository.UserRepository;
import com.mr.global.apipayload.exception.GeneralException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatisticsService {
    private static final int SCORE_SCALE = 1;
    private static final BigDecimal ZERO_SCORE = BigDecimal.ZERO.setScale(SCORE_SCALE);
    private static final int WEEKLY_TREND_WEEKS = 4;

    private final UserRepository userRepository;
    private final PracticeStatisticsRepository practiceStatisticsRepository;
    private final SkillStatisticsRepository skillStatisticsRepository;
    private final Clock clock;

    public StatisticsResponseDTO getStatistics(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorStatus.USER_NOT_FOUND));
        LocalDate thisWeek = LocalDate.now(clock).with(DayOfWeek.MONDAY);
        LocalDate lastWeek = thisWeek.minusWeeks(1);
        LocalDate trendStart = thisWeek.minusWeeks(WEEKLY_TREND_WEEKS - 1L);

        Map<LocalDate, PracticeStatistics> practices = practiceStatisticsRepository
                .findAllByUser_UserIdAndPeriodTypeAndPeriodStartBetween(
                        userId, PeriodType.WEEKLY, trendStart, thisWeek)
                .stream().collect(Collectors.toMap(PracticeStatistics::getPeriodStart, Function.identity()));
        Map<SkillType, SkillStatistics> currentSkills = new EnumMap<>(SkillType.class);
        Map<SkillType, SkillStatistics> previousSkills = new EnumMap<>(SkillType.class);
        skillStatisticsRepository.findAllByUser_UserIdAndPeriodTypeAndPeriodStartBetween(
                        userId, PeriodType.WEEKLY, lastWeek, thisWeek)
                .forEach(statistics -> {
                    if (statistics.getPeriodStart().equals(thisWeek)) {
                        currentSkills.put(statistics.getSkillType(), statistics);
                    } else if (statistics.getPeriodStart().equals(lastWeek)) {
                        previousSkills.put(statistics.getSkillType(), statistics);
                    }
                });

        return new StatisticsResponseDTO(
                buildWeeklySummary(practices.get(thisWeek), practices.get(lastWeek)),
                buildDomainGrowth(currentSkills, previousSkills),
                buildWeeklyTrend(practices, thisWeek));
    }

    private WeeklySummary buildWeeklySummary(PracticeStatistics current, PracticeStatistics previous) {
        BigDecimal currentAccuracy = accuracyOf(current);
        BigDecimal previousAccuracy = accuracyOf(previous);
        int currentMinutes = practiceMinutesOf(current);
        int previousMinutes = practiceMinutesOf(previous);
        int currentCount = sessionCountOf(current);
        int previousCount = sessionCountOf(previous);
        return new WeeklySummary(scoreOrZero(currentAccuracy), currentMinutes, currentCount,
                diffOrZero(currentAccuracy, previousAccuracy),
                currentMinutes - previousMinutes, currentCount - previousCount);
    }

    private List<DomainGrowth> buildDomainGrowth(Map<SkillType, SkillStatistics> current,
            Map<SkillType, SkillStatistics> previous) {
        List<DomainGrowth> result = new ArrayList<>();
        for (SkillType type : SkillType.values()) {
            BigDecimal currentScore = scoreOf(current.get(type));
            BigDecimal previousScore = scoreOf(previous.get(type));
            result.add(new DomainGrowth(type, resolveLabel(type), scoreOrZero(currentScore),
                    scoreOrZero(previousScore), diffOrZero(currentScore, previousScore)));
        }
        return result;
    }

    private WeeklyTrend buildWeeklyTrend(Map<LocalDate, PracticeStatistics> practices, LocalDate thisWeek) {
        List<TrendItem> items = new ArrayList<>();
        for (int weeksAgo = WEEKLY_TREND_WEEKS - 1; weeksAgo >= 0; weeksAgo--) {
            BigDecimal score = accuracyOf(practices.get(thisWeek.minusWeeks(weeksAgo)));
            items.add(new TrendItem(resolveWeekLabel(weeksAgo), scoreOrZero(score)));
        }
        BigDecimal current = accuracyOf(practices.get(thisWeek));
        BigDecimal previous = accuracyOf(practices.get(thisWeek.minusWeeks(1)));
        return new WeeklyTrend(diffIntOrZero(current, previous), items);
    }

    private BigDecimal accuracyOf(PracticeStatistics statistics) {
        return statistics != null ? statistics.getAverageAccuracy() : null;
    }

    private BigDecimal scoreOf(SkillStatistics statistics) {
        return statistics != null ? statistics.getScore() : null;
    }

    private int practiceMinutesOf(PracticeStatistics statistics) {
        return statistics != null && statistics.getPracticeMinutes() != null ? statistics.getPracticeMinutes() : 0;
    }

    private int sessionCountOf(PracticeStatistics statistics) {
        return statistics != null && statistics.getSessionCount() != null ? statistics.getSessionCount() : 0;
    }

    private BigDecimal scoreOrZero(BigDecimal score) {
        return score != null ? score.setScale(SCORE_SCALE, RoundingMode.HALF_UP) : ZERO_SCORE;
    }

    private BigDecimal diffOrZero(BigDecimal current, BigDecimal previous) {
        return current == null || previous == null ? ZERO_SCORE
                : current.subtract(previous).setScale(SCORE_SCALE, RoundingMode.HALF_UP);
    }

    private int diffIntOrZero(BigDecimal current, BigDecimal previous) {
        return current == null || previous == null ? 0
                : current.subtract(previous).setScale(0, RoundingMode.HALF_UP).intValue();
    }

    private String resolveLabel(SkillType type) {
        return switch (type) {
            case SCALE -> "스케일";
            case TENSION -> "텐션";
            case PROGRESSION -> "진행";
            case VOICE_LEADING -> "코드 연결";
        };
    }

    private String resolveWeekLabel(int weeksAgo) {
        return switch (weeksAgo) {
            case 3 -> "3주 전";
            case 2 -> "2주 전";
            case 1 -> "지난주";
            default -> "이번주";
        };
    }
}
