package com.mr.global.event;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PlayingCompletedEvent {
    private Long userId;

    // userId는 완료 처리된 Playing의 소유자(playing.getUser().getUserId())에서만 채울 것 - 클라이언트 입력 금지
    public static PlayingCompletedEvent of(Long userId) {
        return new PlayingCompletedEvent(userId);
    }
}
