package com.mr.global.event;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AnalysisCompletedEvent {
    private Long userId;

    // userId는 완료 처리된 Analysis의 소유자(analysis.getUser().getUserId())에서만 채울 것 - 클라이언트 입력 금지
    public static AnalysisCompletedEvent of(Long userId) {
        return new AnalysisCompletedEvent(userId);
    }
}
