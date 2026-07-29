package com.mr.domain.playing.dto.req;

import com.mr.domain.playing.entity.enums.MidiType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

public record MidiEventSaveRequest (
        @NotEmpty(message = "MIDI 이벤트 목록은 필수입니다")
        @Size(max = 100_000, message = "MIDI 이벤트 개수가 허용 범위를 초과했습니다.")
        List<@Valid MidiEventRequest> events
) {

    public record MidiEventRequest (

        @NotNull(message = "MIDI 이벤트 순서 값은 필수입니다.")
        @Min(value = 0, message = "MIDI 이벤트 순서 값은 0 이상이어야 합니다.")
        Integer sequence,

        @NotNull(message = "MIDI 이벤트 타입은 필수입니다.")
        MidiType type,

        @NotNull(message = "MIDI 피치 값은 필수입니다.")
        @Min(value = 0, message = "MIDI 피치 값은 0 이상이어야 합니다.")
        @Max(value = 127, message = "MIDI 피치 값은 127 이하여야 합니다.")
        Integer pitch,

        @NotNull(message = "MIDI 입력 강도 값은 필수입니다.")
        @Min(value = 0, message = "MIDI 입력 강도 값은 0 이상이어야 합니다.")
        @Max(value = 127, message = "MIDI 입력 강도 값은 127 이하여야 합니다.")
        Integer velocity,

        @NotNull(message = "MIDI 이벤트 발생 시간은 필수입니다.")
        @Min(value = 0, message = "MIDI 이벤트 발생 시간은 0 이상이어야 합니다.")
        Long timestampMs
    ) {
    }
}
