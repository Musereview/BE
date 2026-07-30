package com.mr.domain.statistics.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.mr.global.event.PlayingCompletedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.beans.factory.annotation.Autowired;

// @Retryable/@Recover는 AOP 프록시를 거쳐야만 동작하므로, Mockito만으로 new해서 직접 호출하면
// 재시도 자체가 발생하지 않아 검증할 수 없다. 최소한의 스프링 컨텍스트로 프록시를 실제로 태운다.
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        StatisticsEventListener.class,
        StatisticsEventListenerRetryTest.RetryConfig.class
})
class StatisticsEventListenerRetryTest {

    @EnableRetry
    @Configuration
    static class RetryConfig {
    }

    @MockBean
    private StatisticsAggregationService statisticsAggregationService;

    @Autowired
    private StatisticsEventListener listener;

    @Test
    @DisplayName("유니크 제약 충돌로 두 번 실패해도 세 번째 시도에서 성공하면 정상 종료된다")
    void retriesAndSucceedsEventually() {
        willThrow(new DataIntegrityViolationException("dup"))
                .willThrow(new DataIntegrityViolationException("dup"))
                .willDoNothing()
                .given(statisticsAggregationService).onPlayingCompleted(1L);

        assertThatCode(() -> listener.handlePlayingCompleted(PlayingCompletedEvent.of(1L)))
                .doesNotThrowAnyException();

        verify(statisticsAggregationService, times(3)).onPlayingCompleted(1L);
    }

    @Test
    @DisplayName("재시도를 모두 소진하면 예외를 삼키고 @Recover로 종료해 호출부로 전파되지 않는다")
    void exhaustsRetriesThenRecovers() {
        willThrow(new DataIntegrityViolationException("dup"))
                .given(statisticsAggregationService).onPlayingCompleted(1L);

        assertThatCode(() -> listener.handlePlayingCompleted(PlayingCompletedEvent.of(1L)))
                .doesNotThrowAnyException();

        verify(statisticsAggregationService, times(3)).onPlayingCompleted(1L);
    }
}
