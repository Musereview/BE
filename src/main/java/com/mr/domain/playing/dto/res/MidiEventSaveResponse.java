package com.mr.domain.playing.dto.res;

public record MidiEventSaveResponse(
        Long playingId,
        int savedCount
) {

    public static MidiEventSaveResponse of(Long playingId, int savedCount) {
        return new MidiEventSaveResponse(playingId, savedCount);
    }
}
