package com.mr.domain.playing.dto.res;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.mr.domain.playing.entity.Playing;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public record PlayingDeleteResponse(
        Long playingId,

        @Schema(description = "삭제 일시 (KST 기준 응답)", example = "2026-08-11T18:00:00", type = "string")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Asia/Seoul")
        Instant deletedAt
) {

    public static PlayingDeleteResponse from(Playing playing) {
        return new PlayingDeleteResponse(
                playing.getId(),
                playing.getDeletedAt()
        );
    }
}