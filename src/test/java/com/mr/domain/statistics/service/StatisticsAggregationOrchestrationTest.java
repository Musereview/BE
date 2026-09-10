package com.mr.domain.statistics.service;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class StatisticsAggregationOrchestrationTest {

    private UserStatisticsAggregationService userStatisticsAggregationService;
    private WeeklyPracticeStatisticsAggregationService weeklyPracticeStatisticsAggregationService;
    private WeeklySkillStatisticsAggregationService weeklySkillStatisticsAggregationService;
    private StatisticsAggregationService service;

    @BeforeEach
    void setUp() {
        userStatisticsAggregationService = mock(UserStatisticsAggregationService.class);
        weeklyPracticeStatisticsAggregationService = mock(WeeklyPracticeStatisticsAggregationService.class);
        weeklySkillStatisticsAggregationService = mock(WeeklySkillStatisticsAggregationService.class);
        service = new StatisticsAggregationService(
                userStatisticsAggregationService,
                weeklyPracticeStatisticsAggregationService,
                weeklySkillStatisticsAggregationService);
    }

    @Test
    @DisplayName("연주 완료 시 사용자 누적 통계와 주간 연습 통계를 순서대로 집계한다")
    void onPlayingCompleted_aggregatesUserAndWeeklyPracticeStatistics() {
        service.onPlayingCompleted(1L);

        InOrder inOrder = inOrder(userStatisticsAggregationService, weeklyPracticeStatisticsAggregationService);
        inOrder.verify(userStatisticsAggregationService).aggregatePractice(1L);
        inOrder.verify(weeklyPracticeStatisticsAggregationService).aggregate(1L);
        verifyNoInteractions(weeklySkillStatisticsAggregationService);
    }

    @Test
    @DisplayName("분석 완료 시 사용자 누적, 주간 연습, 주간 스킬 통계를 순서대로 집계한다")
    void onAnalysisCompleted_aggregatesAllStatistics() {
        service.onAnalysisCompleted(1L);

        InOrder inOrder = inOrder(
                userStatisticsAggregationService,
                weeklyPracticeStatisticsAggregationService,
                weeklySkillStatisticsAggregationService);
        inOrder.verify(userStatisticsAggregationService).aggregateAnalysis(1L);
        inOrder.verify(weeklyPracticeStatisticsAggregationService).aggregate(1L);
        inOrder.verify(weeklySkillStatisticsAggregationService).aggregate(1L);
    }
}
