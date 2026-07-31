package com.mr.global.event;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PlayingCompletedEvent {
    private Long userId;

    // 클라이언트 입력이 아닌 완료된 Playing 소유자 식별자
    public static PlayingCompletedEvent of(Long userId) {
        return new PlayingCompletedEvent(userId);
    }
}
