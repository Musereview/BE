package com.mr.global.event;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AnalysisCompletedEvent {
    private Long userId;

    // 완료 분석 소유자 식별자
    public static AnalysisCompletedEvent of(Long userId) {
        return new AnalysisCompletedEvent(userId);
    }
}
