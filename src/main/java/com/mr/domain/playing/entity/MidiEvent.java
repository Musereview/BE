package com.mr.domain.playing.entity;

import com.mr.domain.playing.entity.enums.MidiType;
import com.mr.domain.playing.exception.PlayingErrorStatus;
import com.mr.global.apipayload.exception.GeneralException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(
        name = "midi_event",
        indexes = {
                @Index(name = "idx_midi_event_playing_timestamp",
                        columnList = "playing_id, timestamp_ms")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MidiEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "midi_event_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "playing_id", nullable = false)
    private Playing playing;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private MidiType type;

    @Column(name = "pitch", nullable = false)
    private Integer pitch;

    @Column(name = "velocity", nullable = false)
    private Integer velocity;

    @Column(name = "timestamp_ms", nullable = false)
    private Long timestampMs;

    @Builder(access = AccessLevel.PRIVATE)
    private MidiEvent(
            Playing playing, MidiType type, Integer pitch, Integer velocity, Long timestampMs
    ) {
        validatePlaying(playing);
        validateMidiType(type);
        validatePitch(pitch);
        validateVelocity(velocity);
        validateTimestampMs(timestampMs);

        this.playing = playing;
        this.type = type;
        this.pitch = pitch;
        this.velocity = velocity;
        this.timestampMs = timestampMs;
    }

    private static void validatePlaying(Playing playing) {
        if (playing == null) {
            throw new GeneralException(PlayingErrorStatus.MISSING_PLAYING);
        }
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

    public static MidiEvent create(
            Playing playing, MidiType type, Integer pitch, Integer velocity, Long timestampMs
    ) {
        return MidiEvent.builder()
                .playing(playing)
                .type(type)
                .pitch(pitch)
                .velocity(velocity)
                .timestampMs(timestampMs)
                .build();
    }
}
