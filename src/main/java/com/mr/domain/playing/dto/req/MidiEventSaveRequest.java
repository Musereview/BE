package com.mr.domain.playing.dto.req;

import com.mr.domain.playing.entity.enums.MidiType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.util.List;

import static com.mr.domain.playing.constant.MidiEventConstants.MAX_MIDI_EVENT_COUNT;

public record MidiEventSaveRequest (
        @NotEmpty(message = "MIDI 이벤트 목록은 필수입니다")
        @Size(max = MAX_MIDI_EVENT_COUNT, message = "MIDI 이벤트는 최대 100,000개까지 저장할 수 있습니다.")
        List<@NotNull @Valid MidiEventRequest> events
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
