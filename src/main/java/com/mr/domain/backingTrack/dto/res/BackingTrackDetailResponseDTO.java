package com.mr.domain.backingTrack.dto.res;

import java.util.List;

public class BackingTrackDetailResponseDTO {

    public record DetailResponseDTO(
            Long backingTrackId,
            String title,
            String genre,
            String keySignature,
            String scaleType,
            String timeSignature,
            Integer bpm,
            Integer playtimeSec,
            String level,
            String creatorName,
            String audioFileUrl,
            List<ChordDetail> chordProgression
    ) {
        public static DetailResponseDTO of(Long backingTrackId, String title, String genre, String keySignature,
                                           String scaleType, String timeSignature, Integer bpm, Integer playtimeSec,
                                           String level, String creatorName, String audioFileUrl, List<ChordDetail> chordProgression) {
            return new DetailResponseDTO(backingTrackId, title, genre, keySignature, scaleType, timeSignature, bpm, playtimeSec, level, creatorName, audioFileUrl, chordProgression);
        }
    }

    public record ChordDetail(
            Integer measureNo,
            Integer sequenceNo,
            String chordName
    ) {
        public static ChordDetail of(Integer measureNo, Integer sequenceNo, String chordName) {
            return new ChordDetail(measureNo, sequenceNo, chordName);
        }
    }
}
