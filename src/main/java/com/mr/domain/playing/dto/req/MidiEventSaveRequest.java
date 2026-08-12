package com.mr.domain.playing.dto.req;

import com.mr.domain.playing.entity.enums.MidiType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

import static com.mr.domain.playing.constant.MidiEventConstants.MAX_MIDI_EVENT_COUNT;

public record MidiEventSaveRequest (

        @Schema(description = "연주 중 수집한 MIDI 이벤트 목록")
        @NotEmpty(message = "MIDI 이벤트 목록은 필수입니다")
        @Size(max = MAX_MIDI_EVENT_COUNT, message = "MIDI 이벤트는 최대 100,000개까지 저장할 수 있습니다.")
        List<@NotNull @Valid MidiEventRequest> events,

        @Schema(
                description = "업로드된 녹음 파일 Object Key",
                example = "recordings/34/2026-08-12/161800_a1b2c3.webm"
        )
        @NotBlank(message = "녹음 파일 Object Key는 필수입니다.")
        String recordingObjectKey
) {

    public record MidiEventRequest (

        @Schema(description = "MIDI 이벤트 순서", example = "0")
        @NotNull(message = "MIDI 이벤트 순서 값은 필수입니다.")
        @Min(value = 0, message = "MIDI 이벤트 순서 값은 0 이상이어야 합니다.")
        Integer sequence,

        @Schema(description = "MIDI 이벤트 타입", example = "NOTE_ON")
        @NotNull(message = "MIDI 이벤트 타입은 필수입니다.")
        MidiType type,

        @Schema(description = "MIDI 피치 값", example = "60")
        @NotNull(message = "MIDI 피치 값은 필수입니다.")
        @Min(value = 0, message = "MIDI 피치 값은 0 이상이어야 합니다.")
        @Max(value = 127, message = "MIDI 피치 값은 127 이하여야 합니다.")
        Integer pitch,

        @Schema(description = "MIDI 입력 강도", example = "100")
        @NotNull(message = "MIDI 입력 강도 값은 필수입니다.")
        @Min(value = 0, message = "MIDI 입력 강도 값은 0 이상이어야 합니다.")
        @Max(value = 127, message = "MIDI 입력 강도 값은 127 이하여야 합니다.")
        Integer velocity,

        @Schema(description = "연주 시작 기준 MIDI 이벤트 발생 시간(ms)", example = "1250")
        @NotNull(message = "MIDI 이벤트 발생 시간은 필수입니다.")
        @Min(value = 0, message = "MIDI 이벤트 발생 시간은 0 이상이어야 합니다.")
        Long timestampMs
    ) {
    }
}
