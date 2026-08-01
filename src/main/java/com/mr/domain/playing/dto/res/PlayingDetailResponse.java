package com.mr.domain.playing.dto.res;

import com.mr.domain.backingtrack.entity.BackingTrack;
import com.mr.domain.backingtrack.entity.enums.ScaleType;
import com.mr.domain.playing.entity.Playing;
import com.mr.domain.playing.entity.enums.PlayingMode;
import com.mr.domain.playing.entity.enums.PlayingStatus;

import java.time.LocalDateTime;

public record PlayingDetailResponse(
        Long playingId,
        PlayingStatus status,
        PlayingMode mode,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        Integer duration,
        Integer bpm,
        String recordingFileUrl,
        boolean isPublic,
        BackingTrackInfo backingTrack,
        LocalDateTime createdAt
) {
    public static PlayingDetailResponse from(Playing playing) {
        return new PlayingDetailResponse(
                playing.getId(),
                playing.getStatus(),
                playing.getMode(),
                playing.getStartedAt(),
                playing.getEndedAt(),
                playing.getDurationSec(),
                playing.getBpm(),
                playing.getRecordingFileUrl(),
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
