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
            List<String> chordProgression, // 피그마 UI: "Cmaj7 | Am7 | Dm7 | G7" 표기용
            Integer bpm,
            String level,
            Integer playtimeSec
    ) {
        public static TrackInfo of(Long backingTrackId, String title, String genre, String keySignature,
                                   String scaleType, List<String> chordProgression, Integer bpm,
                                   String level, Integer playtimeSec) {
            return new TrackInfo(backingTrackId, title, genre, keySignature, scaleType, chordProgression, bpm, level, playtimeSec);
        }
    }
}
