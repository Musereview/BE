package com.mr.domain.playing.dto.res;

import com.mr.domain.playing.entity.Playing;

import java.time.Instant;

public record PlayingDeleteResponse(
        Long playingId,
        Instant deletedAt
) {

    public static PlayingDeleteResponse from(Playing playing) {
        return new PlayingDeleteResponse(
                playing.getId(),
                playing.getDeletedAt()
        );
    }
}
