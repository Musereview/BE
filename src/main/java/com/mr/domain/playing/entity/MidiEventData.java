package com.mr.domain.playing.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.mr.domain.playing.entity.enums.MidiType;
import com.mr.domain.playing.exception.MidiEventErrorStatus;
import com.mr.global.apipayload.exception.GeneralException;
import lombok.Getter;

@Getter
public class MidiEventData {

    // 동일 timestamp 내 순서 값
    @JsonProperty("sequence")
    private final Integer sequence;

    private final MidiType type;
    private final Integer pitch;
    private final Integer velocity;

    @JsonProperty("timestamp_ms")
    private final Long timestampMs;

    private MidiEventData(
            Integer sequence,
            MidiType type,
            Integer pitch,
            Integer velocity,
            Long timestampMs
    ) {
        validateSequence(sequence);
        validateMidiType(type);
        validatePitch(pitch);
        validateVelocity(velocity);
        validateTimestampMs(timestampMs);

        this.sequence = sequence;
        this.type = type;
        this.pitch = pitch;
        this.velocity = velocity;
        this.timestampMs = timestampMs;
    }

    @JsonCreator
    public static MidiEventData of(
            @JsonProperty("sequence") Integer sequence,
            @JsonProperty("type") MidiType type,
            @JsonProperty("pitch") Integer pitch,
            @JsonProperty("velocity") Integer velocity,
            @JsonProperty("timestamp_ms") Long timestampMs
    ) {
        return new MidiEventData(sequence, type, pitch, velocity, timestampMs);
    }

    private static void validateSequence(Integer sequence) {
        if (sequence == null || sequence < 0) {
            throw new GeneralException(MidiEventErrorStatus.INVALID_MIDI_SEQUENCE);
        }
    }

    private static void validateMidiType(MidiType type) {
        if (type == null) {
            throw new GeneralException(MidiEventErrorStatus.INVALID_MIDI_TYPE);
        }
    }

    private static void validatePitch(Integer pitch) {
        if (pitch == null || pitch < 0 || pitch > 127) {
            throw new GeneralException(MidiEventErrorStatus.INVALID_PITCH_RANGE);
        }
    }

    private static void validateVelocity(Integer velocity) {
        if (velocity == null || velocity < 0 || velocity > 127) {
            throw new GeneralException(MidiEventErrorStatus.INVALID_VELOCITY_RANGE);
        }
    }

    private static void validateTimestampMs(Long timestampMs) {
        if (timestampMs == null || timestampMs < 0) {
            throw new GeneralException(MidiEventErrorStatus.INVALID_TIMESTAMP);
        }
    }
}
