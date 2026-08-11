package com.mr.domain.playing.dto.res;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.mr.domain.backingtrack.entity.BackingTrack;
import com.mr.domain.backingtrack.entity.enums.ScaleType;
import com.mr.domain.playing.entity.Playing;
import com.mr.domain.playing.entity.enums.PlayingMode;
import com.mr.domain.playing.entity.enums.PlayingStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public record PlayingDetailResponse(
        Long playingId,
        PlayingStatus status,
        PlayingMode mode,

        @Schema(description = "시작 일시 (KST 기준 응답)", example = "2026-08-11T18:00:00", type = "string")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Asia/Seoul")
        Instant startedAt,

        @Schema(description = "종료 일시 (KST 기준 응답)", example = "2026-08-11T18:00:00", type = "string")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Asia/Seoul")
        Instant endedAt,

        Integer duration,
        Integer bpm,
        String recordingFileUrl,
        boolean isPublic,
        BackingTrackInfo backingTrack,

        @Schema(description = "생성 일시 (KST 기준 응답)", example = "2026-08-11T18:00:00", type = "string")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Asia/Seoul")
        Instant createdAt
) {
    public static PlayingDetailResponse from(Playing playing, String recordingFileUrl) {
        return new PlayingDetailResponse(
                playing.getId(),
                playing.getStatus(),
                playing.getMode(),
                playing.getStartedAt(),
                playing.getEndedAt(),
                playing.getDurationSec(),
                playing.getBpm(),
                recordingFileUrl,
                playing.isPublic(),
                BackingTrackInfo.from(playing.getBackingTrack()),
                playing.getCreatedAt()
        );
    }

    public record BackingTrackInfo(
            Long backingTrackId,
            String title,
            String genre,
            String keySignature,
            ScaleType scaleType,
            String timeSignature,
            Integer playtimeSec
    ){
        public static BackingTrackInfo from(BackingTrack backingTrack) {
            if (backingTrack == null) return null;

            return new BackingTrackInfo(
                    backingTrack.getId(),
                    backingTrack.getTitle(),
                    backingTrack.getGenre(),
                    backingTrack.getKeySignature(),
                    backingTrack.getScaleType(),
                    backingTrack.getTimeSignature(),
                    backingTrack.getPlaytimeSec()
            );
        }
    }
}