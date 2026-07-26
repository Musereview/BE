package com.mr.global.client.ai;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record AiAnalysisRequest(
        Meta meta,
        List<Chord> chords,
        List<Note> notes
) {
    public record Meta(
            Double bpm,
            @JsonProperty("time_signature") List<Integer> timeSignature,
            Key key,
            String genre
    ) {
    }

    public record Key(
            String tonic,
            String mode
    ) {
    }

    public record Chord(
            Integer bar,
            Double beat,
            String symbol
    ) {
    }

    public record Note(
            Integer index,
            Integer pitch,
            @JsonProperty("onset_beats") Double onsetBeats,
            @JsonProperty("duration_beats") Double durationBeats,
            Integer velocity
    ) {
    }
}
