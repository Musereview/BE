package com.mr.global.event;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PlayingCompletedEvent {
    private Long userId;

    // 완료 연주 소유자 식별자
    public static PlayingCompletedEvent of(Long userId) {
        return new PlayingCompletedEvent(userId);
    }
}
