package com.mr.domain.statistics.service;

import com.mr.domain.analysis.entity.Analysis;
import com.mr.domain.analysis.entity.enums.AnalysisStatus;
import com.mr.domain.analysis.repository.AnalysisRepository;
import com.mr.domain.playing.entity.Playing;
import com.mr.domain.playing.entity.enums.PlayingStatus;
import com.mr.domain.playing.repository.PlayingRepository;
import com.mr.domain.statistics.dto.res.StatisticsResponseDTO;
import com.mr.domain.statistics.dto.res.StatisticsResponseDTO.DomainGrowth;
import com.mr.domain.statistics.dto.res.StatisticsResponseDTO.TrendItem;
import com.mr.domain.statistics.dto.res.StatisticsResponseDTO.WeeklySummary;
import com.mr.domain.statistics.dto.res.StatisticsResponseDTO.WeeklyTrend;
import com.mr.domain.statistics.entity.enums.SkillType;
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
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
    private static final int SECONDS_PER_MINUTE = 60;

    private final UserRepository userRepository;
    private final PlayingRepository playingRepository;
    private final AnalysisRepository analysisRepository;
    private final Clock clock;

    public StatisticsResponseDTO getStatistics(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorStatus.USER_NOT_FOUND));

        ZoneId kstZone = ZoneId.of("Asia/Seoul");
        Instant thisWeekStart = LocalDate.now(kstZone)
                .with(DayOfWeek.MONDAY)
                .atStartOfDay(kstZone)
                .toInstant();

        Instant lastWeekStart = thisWeekStart.minus(7, ChronoUnit.DAYS);
        Instant fourWeeksAgoStart = thisWeekStart.minus((WEEKLY_TREND_WEEKS - 1L) * 7, ChronoUnit.DAYS);

        List<Playing> playings =
                playingRepository.findByUserAndStatusSince(userId, PlayingStatus.COMPLETED, lastWeekStart);
        List<Analysis> analyses =
                analysisRepository.findByUserAndStatusSince(userId, AnalysisStatus.COMPLETED, fourWeeksAgoStart);

        ScoreAggregate[] weeklyScoreAggregates = buildWeeklyScoreAggregates(analyses, thisWeekStart);

        return new StatisticsResponseDTO(
                buildWeeklySummary(playings, weeklyScoreAggregates, thisWeekStart, lastWeekStart),
                buildDomainGrowth(analyses, thisWeekStart, lastWeekStart),
                buildWeeklyTrend(weeklyScoreAggregates)
        );
    }

    private ScoreAggregate[] buildWeeklyScoreAggregates(List<Analysis> analyses, Instant thisWeekStart) {
        ScoreAggregate[] aggregates = new ScoreAggregate[WEEKLY_TREND_WEEKS];
        for (int weeksAgo = 0; weeksAgo < WEEKLY_TREND_WEEKS; weeksAgo++) {
            Instant from = thisWeekStart.minus(weeksAgo * 7L, ChronoUnit.DAYS);
            Instant toExclusive = from.plus(7, ChronoUnit.DAYS);
            aggregates[weeksAgo] = aggregateTotalScore(analyses, from, toExclusive);
        }
        return aggregates;
    }

    private WeeklySummary buildWeeklySummary(List<Playing> playings, ScoreAggregate[] weeklyScoreAggregates,
            Instant thisWeekStart, Instant lastWeekStart) {
        int thisWeekMinutes = sumMinutes(playings, thisWeekStart, null);
        int lastWeekMinutes = sumMinutes(playings, lastWeekStart, thisWeekStart);
        int thisWeekCount = countInRange(playings, thisWeekStart, null);
        int lastWeekCount = countInRange(playings, lastWeekStart, thisWeekStart);

        ScoreAggregate thisWeekScore = weeklyScoreAggregates[0];
        ScoreAggregate lastWeekScore = weeklyScoreAggregates[1];

        return new WeeklySummary(
                thisWeekScore.average(),
                thisWeekMinutes,
                thisWeekCount,
                diffOrZero(thisWeekScore, lastWeekScore),
                thisWeekMinutes - lastWeekMinutes,
                thisWeekCount - lastWeekCount
        );
    }

    private List<DomainGrowth> buildDomainGrowth(List<Analysis> analyses,
            Instant thisWeekStart, Instant lastWeekStart) {
        List<DomainGrowth> result = new ArrayList<>();
        for (SkillType skillType : SkillType.values()) {
            ScoreAggregate current = aggregateSkillScore(analyses, skillType, thisWeekStart, null);
            ScoreAggregate previous = aggregateSkillScore(analyses, skillType, lastWeekStart, thisWeekStart);

            result.add(new DomainGrowth(
                    skillType,
                    resolveLabel(skillType),
                    current.average(),
                    previous.average(),
                    diffOrZero(current, previous)
            ));
        }
        return result;
    }

    private WeeklyTrend buildWeeklyTrend(ScoreAggregate[] weeklyScoreAggregates) {
        List<TrendItem> items = new ArrayList<>();
        for (int weeksAgo = WEEKLY_TREND_WEEKS - 1; weeksAgo >= 0; weeksAgo--) {
            items.add(new TrendItem(resolveWeekLabel(weeksAgo), weeklyScoreAggregates[weeksAgo].average()));
        }

        int diffFromPreviousWeek = diffIntOrZero(weeklyScoreAggregates[0], weeklyScoreAggregates[1]);
        return new WeeklyTrend(diffFromPreviousWeek, items);
    }

    private ScoreAggregate aggregateTotalScore(List<Analysis> analyses, Instant from, Instant toExclusive) {
        List<Integer> scores = analyses.stream()
                .filter(a -> isWithin(a.getCompletedAt(), from, toExclusive))
                .map(Analysis::getTotalScore)
                .filter(Objects::nonNull)
                .toList();

        BigDecimal sum = scores.stream().map(BigDecimal::valueOf).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new ScoreAggregate(sum, scores.size());
    }

    private ScoreAggregate aggregateSkillScore(List<Analysis> analyses, SkillType skillType,
            Instant from, Instant toExclusive) {
        List<BigDecimal> scores = analyses.stream()
                .filter(a -> isWithin(a.getCompletedAt(), from, toExclusive))
                .map(a -> AnalysisSkillScoreResolver.resolve(a, skillType))
                .filter(Objects::nonNull)
                .toList();

        BigDecimal sum = scores.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return new ScoreAggregate(sum, scores.size());
    }

    private boolean isWithin(Instant target, Instant from, Instant toExclusive) {
        if (target == null || target.isBefore(from)) {
            return false;
        }
        return toExclusive == null || target.isBefore(toExclusive);
    }

    private int sumMinutes(List<Playing> playings, Instant from, Instant toExclusive) {
        int totalSeconds = playings.stream()
                .filter(p -> isWithin(p.getEndedAt(), from, toExclusive))
                .mapToInt(p -> p.getDurationSec() != null ? p.getDurationSec() : 0)
                .sum();
        return totalSeconds / SECONDS_PER_MINUTE;
    }

    private int countInRange(List<Playing> playings, Instant from, Instant toExclusive) {
        return (int) playings.stream()
                .filter(p -> isWithin(p.getEndedAt(), from, toExclusive))
                .count();
    }

    private String resolveLabel(SkillType skillType) {
        return switch (skillType) {
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

    private BigDecimal diffOrZero(ScoreAggregate current, ScoreAggregate previous) {
        if (current.count() == 0 || previous.count() == 0) {
            return ZERO_SCORE;
        }
        return current.average().subtract(previous.average());
    }

    private int diffIntOrZero(ScoreAggregate current, ScoreAggregate previous) {
        if (current.count() == 0 || previous.count() == 0) {
            return 0;
        }
        return current.average().subtract(previous.average())
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();
    }

    private record ScoreAggregate(BigDecimal sum, int count) {
        BigDecimal average() {
            if (count == 0) {
                return ZERO_SCORE;
            }
            return sum.divide(BigDecimal.valueOf(count), SCORE_SCALE, RoundingMode.HALF_UP);
        }
    }
}
