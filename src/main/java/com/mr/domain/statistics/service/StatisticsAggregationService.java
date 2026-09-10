package com.mr.domain.statistics.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StatisticsAggregationService {

    private final UserStatisticsAggregationService userStatisticsAggregationService;
    private final WeeklyPracticeStatisticsAggregationService weeklyPracticeStatisticsAggregationService;
    private final WeeklySkillStatisticsAggregationService weeklySkillStatisticsAggregationService;

    // 재시도별 신규 트랜잭션 보장
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onPlayingCompleted(Long userId) {
        userStatisticsAggregationService.aggregatePractice(userId);
        weeklyPracticeStatisticsAggregationService.aggregate(userId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onAnalysisCompleted(Long userId) {
        userStatisticsAggregationService.aggregateAnalysis(userId);
        weeklyPracticeStatisticsAggregationService.aggregate(userId);
        weeklySkillStatisticsAggregationService.aggregate(userId);
    }
}
