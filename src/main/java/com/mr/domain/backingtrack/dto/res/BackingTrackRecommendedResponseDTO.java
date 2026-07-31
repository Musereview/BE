package com.mr.domain.backingtrack.dto.res;

import java.util.List;

public class BackingTrackRecommendedResponseDTO {

    public record RecommendedResponseDTO(
            List<TrackInfo> recommendedTracks
    ) {
        public static RecommendedResponseDTO of(List<TrackInfo> recommendedTracks) {
            return new RecommendedResponseDTO(recommendedTracks);
        }
    }

    public record TrackInfo(
            Long backingTrackId,
            String title,
            String genre,
            String keySignature,
            String scaleType,
            String timeSignature,
            List<String> chordProgression,
            Integer bpm,
            String level,
            Integer playtimeSec,
            Integer playCount
    ) {
        public static TrackInfo of(Long backingTrackId, String title, String genre, String keySignature,
                                   String scaleType, String timeSignature, List<String> chordProgression,
                                   Integer bpm, String level, Integer playtimeSec, Integer playCount) {
            return new TrackInfo(backingTrackId, title, genre, keySignature, scaleType, timeSignature,
                    chordProgression, bpm, level, playtimeSec, playCount);
        }
    }
}
