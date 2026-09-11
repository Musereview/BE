package com.mr.domain.statistics.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.mr.domain.analysis.entity.Analysis;
import com.mr.domain.analysis.repository.AnalysisRepository;
import com.mr.domain.backingtrack.entity.BackingTrack;
import com.mr.domain.backingtrack.entity.enums.AccessLevel;
import com.mr.domain.backingtrack.entity.enums.Level;
import com.mr.domain.backingtrack.entity.enums.ScaleType;
import com.mr.domain.backingtrack.repository.BackingTrackRepository;
import com.mr.domain.playing.entity.Playing;
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
import jakarta.persistence.EntityManagerFactory;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(properties = {
        "spring.jpa.properties.hibernate.generate_statistics=true",
        "spring.jpa.properties.hibernate.session.events.log=false"
})
@ActiveProfiles("test")
@Import(StatisticsAggregationPerformanceTest.FixedClockConfig.class)
class StatisticsAggregationPerformanceTest {

    private static final int WARM_UP_COUNT = 3;
    private static final int MEASUREMENT_COUNT = 10;
    private static final long MAX_ENTITY_LOAD_COUNT = 6L;
    private static final ZoneId KOREA_ZONE_ID = ZoneId.of("Asia/Seoul");
    private static final Instant FIXED_INSTANT = Instant.parse("2026-09-10T00:00:00Z");
    private static final BigDecimal EXPECTED_SCORE = new BigDecimal("80.0");

    @Autowired
    private StatisticsAggregationService statisticsAggregationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BackingTrackRepository backingTrackRepository;

    @Autowired
    private PlayingRepository playingRepository;

    @Autowired
    private AnalysisRepository analysisRepository;

    @Autowired
    private UserStatisticsRepository userStatisticsRepository;

    @Autowired
    private PracticeStatisticsRepository practiceStatisticsRepository;

    @Autowired
    private SkillStatisticsRepository skillStatisticsRepository;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @ParameterizedTest(name = "Analysis {0}건")
    @ValueSource(ints = {100, 1_000, 5_000, 10_000})
    void measuresAnalysisCompletedWarmPath(int analysisCount) {
        Long userId = transactionTemplate.execute(status -> prepareFixture(analysisCount));
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();

        for (int iteration = 0; iteration < WARM_UP_COUNT; iteration++) {
            resetWarmStatistics(userId);
            statisticsAggregationService.onAnalysisCompleted(userId);
        }

        List<Measurement> measurements = new ArrayList<>(MEASUREMENT_COUNT);
        for (int iteration = 0; iteration < MEASUREMENT_COUNT; iteration++) {
            measurements.add(measure(statistics, userId));
        }

        assertStableMeasurementCounts(measurements);
        printBaseline(analysisCount, measurements);

        transactionTemplate.executeWithoutResult(status -> verifyAggregatedResult(userId, analysisCount));
    }

    private Measurement measure(Statistics statistics, Long userId) {
        resetWarmStatistics(userId);
        statistics.clear();
        long startedAt = System.nanoTime();
        statisticsAggregationService.onAnalysisCompleted(userId);
        long elapsedNanos = System.nanoTime() - startedAt;
        return new Measurement(
                statistics.getPrepareStatementCount(),
                statistics.getEntityLoadCount(),
                elapsedNanos);
    }

    private void resetWarmStatistics(Long userId) {
        transactionTemplate.executeWithoutResult(status -> {
            LocalDate weekStart = FIXED_INSTANT.atZone(KOREA_ZONE_ID).toLocalDate().with(DayOfWeek.MONDAY);
            UserStatistics userStatistics = userStatisticsRepository.findByUser_UserId(userId).orElseThrow();
            PracticeStatistics practiceStatistics = practiceStatisticsRepository
                    .findByUser_UserIdAndPeriodTypeAndPeriodStart(userId, PeriodType.WEEKLY, weekStart)
                    .orElseThrow();
            List<SkillStatistics> skillStatistics = skillStatisticsRepository
                    .findAllByUser_UserIdAndPeriodTypeAndPeriodStartBetween(
                            userId, PeriodType.WEEKLY, weekStart, weekStart);

            userStatistics.updateAnalysisSummary(0, null);
            practiceStatistics.update(0, 0, null);
            skillStatistics.forEach(statistic -> statistic.updateScore(BigDecimal.ZERO, statistic.getPreviousScore()));
        });
    }

    private Long prepareFixture(int analysisCount) {
        User user = userRepository.save(User.createFromOAuth("https://example.com/profile.png"));
        BackingTrack backingTrack = backingTrackRepository.save(BackingTrack.create(
                user,
                1L,
                "성능 측정 트랙",
                "JAZZ",
                "C",
                ScaleType.MAJOR,
                "4/4",
                120,
                300,
                "backing-tracks/performance-test.mp3",
                null,
                AccessLevel.PRIVATE,
                Level.BASIC));
        Playing playing = playingRepository.save(Playing.createBackingTrack(user, backingTrack, 120));

        List<Analysis> analyses = java.util.stream.IntStream.range(0, analysisCount)
                .mapToObj(index -> createCompletedAnalysis(user, playing))
                .toList();
        analysisRepository.saveAll(analyses);
        prepareWarmStatistics(user);
        return user.getUserId();
    }

    private Analysis createCompletedAnalysis(User user, Playing playing) {
        Analysis analysis = Analysis.createPending(user, playing, 1, 4, "{}");
        analysis.startProcessing(FIXED_INSTANT.minusSeconds(1));
        analysis.complete(
                80,
                null,
                "성능 측정",
                EXPECTED_SCORE,
                EXPECTED_SCORE,
                EXPECTED_SCORE,
                EXPECTED_SCORE,
                "{}",
                FIXED_INSTANT);
        return analysis;
    }

    private void prepareWarmStatistics(User user) {
        LocalDate weekStart = FIXED_INSTANT.atZone(KOREA_ZONE_ID).toLocalDate().with(DayOfWeek.MONDAY);
        LocalDate weekEnd = weekStart.plusDays(6);

        userStatisticsRepository.save(UserStatistics.createForUser(user));
        practiceStatisticsRepository.save(PracticeStatistics.create(
                user, PeriodType.WEEKLY, weekStart, weekEnd));
        skillStatisticsRepository.saveAll(java.util.Arrays.stream(SkillType.values())
                .map(skillType -> SkillStatistics.create(
                        user, PeriodType.WEEKLY, weekStart, weekEnd, skillType, BigDecimal.ZERO))
                .toList());
    }

    private void assertStableMeasurementCounts(List<Measurement> measurements) {
        assertThat(measurements)
                .extracting(Measurement::preparedStatementCount)
                .containsOnly(measurements.get(0).preparedStatementCount());
        assertThat(measurements)
                .extracting(Measurement::entityLoadCount)
                .containsOnly(measurements.get(0).entityLoadCount());
        assertThat(measurements)
                .extracting(Measurement::entityLoadCount)
                .allSatisfy(entityLoadCount -> assertThat(entityLoadCount)
                        .as("Aggregate 집계 경로의 Entity Load 상한")
                        .isLessThanOrEqualTo(MAX_ENTITY_LOAD_COUNT));
    }

    private void printBaseline(int analysisCount, List<Measurement> measurements) {
        double medianElapsedMs = median(measurements.stream()
                .map(Measurement::elapsedNanos)
                .toList()) / 1_000_000.0;
        System.out.printf(
                "[Statistics baseline] analyses=%d, preparedStatements=%d, entityLoads=%d, medianElapsedMs=%.3f%n",
                analysisCount,
                measurements.get(0).preparedStatementCount(),
                measurements.get(0).entityLoadCount(),
                medianElapsedMs);
    }

    private double median(List<Long> values) {
        List<Long> sorted = values.stream().sorted().toList();
        int middle = sorted.size() / 2;
        if (sorted.size() % 2 == 1) {
            return sorted.get(middle);
        }
        return (sorted.get(middle - 1) + sorted.get(middle)) / 2.0;
    }

    private void verifyAggregatedResult(Long userId, int analysisCount) {
        UserStatistics userStatistics = userStatisticsRepository.findByUser_UserId(userId).orElseThrow();
        LocalDate weekStart = FIXED_INSTANT.atZone(KOREA_ZONE_ID).toLocalDate().with(DayOfWeek.MONDAY);
        PracticeStatistics practiceStatistics = practiceStatisticsRepository
                .findByUser_UserIdAndPeriodTypeAndPeriodStart(userId, PeriodType.WEEKLY, weekStart)
                .orElseThrow();
        List<SkillStatistics> skillStatistics = skillStatisticsRepository
                .findAllByUser_UserIdAndPeriodTypeAndPeriodStartBetween(
                        userId, PeriodType.WEEKLY, weekStart, weekStart);

        assertThat(userStatistics.getTotalAnalysisCount()).isEqualTo(analysisCount);
        assertThat(userStatistics.getAverageAccuracy()).isEqualByComparingTo(EXPECTED_SCORE);
        assertThat(practiceStatistics.getAverageAccuracy()).isEqualByComparingTo(EXPECTED_SCORE);
        assertThat(skillStatistics).hasSize(SkillType.values().length);
        assertThat(skillStatistics)
                .extracting(SkillStatistics::getScore)
                .allSatisfy(score -> assertThat(score).isEqualByComparingTo(EXPECTED_SCORE));
    }

    private record Measurement(long preparedStatementCount, long entityLoadCount, long elapsedNanos) {
    }

    @TestConfiguration
    static class FixedClockConfig {

        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(FIXED_INSTANT, KOREA_ZONE_ID);
        }
    }
}
