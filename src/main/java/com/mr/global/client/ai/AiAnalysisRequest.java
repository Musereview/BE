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
            String genre,
            String level
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
            NoteType type,
            Integer pitch,
            Integer velocity,
            @JsonProperty("timestamp_ms") Double timestampMs
    ) {
    }

    public enum NoteType {
        NOTE_ON,
        NOTE_OFF
    }
}
