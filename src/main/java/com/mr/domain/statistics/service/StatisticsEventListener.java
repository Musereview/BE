package com.mr.domain.statistics.service;

import com.mr.global.event.AnalysisCompletedEvent;
import com.mr.global.event.PlayingCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

// 트랜잭션 경계는 StatisticsAggregationService에서 관리(별도 빈으로 분리한 이유는 그쪽 주석 참고)
@Slf4j
@Component
@RequiredArgsConstructor
public class StatisticsEventListener {

    private final StatisticsAggregationService statisticsAggregationService;

    // 유니크 제약 충돌(동시 upsert)만 재시도 대상 - 그 외 예외(유저 없음 등)는 즉시 전파해 조용히 삼키지 않음
    @Retryable(
            retryFor = {DataIntegrityViolationException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000)
    )
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePlayingCompleted(PlayingCompletedEvent event) {
        statisticsAggregationService.onPlayingCompleted(event.getUserId());
    }

    @Retryable(
            retryFor = {DataIntegrityViolationException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000)
    )
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAnalysisCompleted(AnalysisCompletedEvent event) {
        statisticsAggregationService.onAnalysisCompleted(event.getUserId());
    }

    @Recover
    public void recoverPlayingCompleted(DataIntegrityViolationException e, PlayingCompletedEvent event) {
        log.error("[통계 집계 최종 실패] userId: {}, event: PlayingCompleted, 원인: {}",
                event.getUserId(), e.getMessage());
    }

    @Recover
    public void recoverAnalysisCompleted(DataIntegrityViolationException e, AnalysisCompletedEvent event) {
        log.error("[통계 집계 최종 실패] userId: {}, event: AnalysisCompleted, 원인: {}",
                event.getUserId(), e.getMessage());
    }
}
