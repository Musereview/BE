package com.mr.domain.playing.dto.res;

import com.mr.domain.backingtrack.entity.BackingTrack;
import com.mr.domain.backingtrack.entity.ChordProgression;
import com.mr.domain.backingtrack.entity.enums.ScaleType;
import com.mr.domain.playing.entity.Playing;
import com.mr.domain.playing.entity.enums.PlayingStatus;

import java.time.LocalDateTime;
import java.util.List;

public record PlayingStartResponse(
        Long playingId,
        PlayingStatus status,
        BackingTrackResponse backingTrack,
        LocalDateTime startedAt
) {

    public static PlayingStartResponse from (Playing playing) {
        return new PlayingStartResponse(
                playing.getId(),
                playing.getStatus(),
                BackingTrackResponse.from(playing.getBackingTrack()),
                playing.getStartedAt()
        );
    }

    public record BackingTrackResponse(
            Long backingTrackId,
            String title,
            String audioFileUrl,
            String genre,
            String keySignature,
            ScaleType scaleType,
            Integer bpm,
            String timeSignature,
            Integer playtimeSec,
            List<ChordProgressionResponse> chordProgression

    ) {
        public static BackingTrackResponse from(BackingTrack backingTrack) {
            return new BackingTrackResponse(
                    backingTrack.getId(),
                    backingTrack.getTitle(),
                    backingTrack.getAudioFileUrl(),
                    backingTrack.getGenre(),
                    backingTrack.getKeySignature(),
                    backingTrack.getScaleType(),
                    backingTrack.getBpm(),
                    backingTrack.getTimeSignature(),
                    backingTrack.getPlaytimeSec(),
                    backingTrack.getChordProgressions()
                            .stream()
                            .map(ChordProgressionResponse::from)
                            .toList()
            );
        }
    }

    public record ChordProgressionResponse(
            Integer measureNo,
            Integer sequenceNo,
            String chordName
    ) {
        public static ChordProgressionResponse from(ChordProgression chordProgression) {

            return new ChordProgressionResponse(
                    chordProgression.getMeasureNo(),
                    chordProgression.getSequenceNo(),
                    chordProgression.getChordName()
            );
        }
    }
}
