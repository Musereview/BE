package com.mr.domain.playing.entity;

import com.mr.domain.playing.entity.enums.MidiType;
import com.mr.domain.playing.exception.PlayingErrorStatus;
import com.mr.global.apipayload.exception.GeneralException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MidiEventData {

    private MidiType type;
    private Integer pitch;
    private Integer velocity;
    private Long timestampMs;

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

    public static MidiEventData create(
            MidiType type,
            Integer pitch,
            Integer velocity,
            Long timestampMs
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
