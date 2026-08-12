package com.mr.domain.backingtrack.dto.res;

import java.util.List;

public class BackingTrackListResponseDTO {

    public record ListResponseDTO(
            List<TrackInfo> tracks,
            Long nextCursor,
            boolean hasNext
    ) {
        public static ListResponseDTO of(List<TrackInfo> tracks, Long nextCursor, boolean hasNext) {
            return new ListResponseDTO(tracks, nextCursor, hasNext);
        }
    }

    public record TrackInfo(
            Long backingTrackId,
            String title,
            String genre,
            String keySignature,
            String scaleType,
            String timeSignature,
            List<ChordInfo> chordProgression, // List<String> -> List<ChordInfo>로 변경
            Integer bpm,
            String level,
            Integer playtimeSec
    ) {
        public static TrackInfo of(Long backingTrackId, String title, String genre, String keySignature,
                                   String scaleType, String timeSignature, List<ChordInfo> chordProgression, Integer bpm,
                                   String level, Integer playtimeSec) {
            return new TrackInfo(backingTrackId, title, genre, keySignature, scaleType, timeSignature, chordProgression, bpm, level, playtimeSec);
        }
    }

    public record ChordInfo(
            Integer measureNo,
            Integer sequenceNo,
            String chordName
    ) {}
}
