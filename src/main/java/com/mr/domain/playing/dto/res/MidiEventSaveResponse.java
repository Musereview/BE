package com.mr.domain.playing.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;

public record MidiEventSaveResponse(

        @Schema(description = "연주 ID", example = "128")
        Long playingId,

        @Schema(description = "저장된 MIDI 이벤트 개수", example = "256")
        int savedCount
) {

    public static MidiEventSaveResponse of(Long playingId, int savedCount) {
        return new MidiEventSaveResponse(playingId, savedCount);
    }
}
