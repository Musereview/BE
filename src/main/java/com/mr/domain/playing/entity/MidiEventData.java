package com.mr.domain.playing.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.mr.domain.playing.entity.enums.MidiType;
import com.mr.domain.playing.exception.PlayingErrorStatus;
import com.mr.global.apipayload.exception.GeneralException;
import lombok.Getter;

@Getter
public class MidiEventData {

    private final MidiType type;
    private final Integer pitch;
    private final Integer velocity;

    @JsonProperty("timestamp_ms")
    private final Long timestampMs;

    private MidiEventData(
            MidiType type,
            Integer pitch,
            Integer velocity,
            Long timestampMs
    ) {
        validateMidiType(type);
        validatePitch(pitch);
        validateVelocity(velocity);
        validateTimestampMs(timestampMs);

        this.type = type;
        this.pitch = pitch;
        this.velocity = velocity;
        this.timestampMs = timestampMs;
    }

    @JsonCreator
    public static MidiEventData of(
            @JsonProperty("type") MidiType type,
            @JsonProperty("pitch") Integer pitch,
            @JsonProperty("velocity") Integer velocity,
            @JsonProperty("timestamp_ms") Long timestampMs
    ) {
        return new MidiEventData(type, pitch, velocity, timestampMs);
    }

    private static void validateMidiType(MidiType type) {
        if (type == null) {
            throw new GeneralException(PlayingErrorStatus.MISSING_MIDI_TYPE);
        }
    }

    private static void validatePitch(Integer pitch) {
        if (pitch == null || pitch < 0 || pitch > 127) {
            throw new GeneralException(PlayingErrorStatus.INVALID_PITCH_RANGE);
        }
    }

    private static void validateVelocity(Integer velocity) {
        if (velocity == null || velocity < 0 || velocity > 127) {
            throw new GeneralException(PlayingErrorStatus.INVALID_VELOCITY_RANGE);
        }
    }

    private static void validateTimestampMs(Long timestampMs) {
        if (timestampMs == null || timestampMs < 0) {
            throw new GeneralException(PlayingErrorStatus.INVALID_TIMESTAMP);
        }
    }
}
