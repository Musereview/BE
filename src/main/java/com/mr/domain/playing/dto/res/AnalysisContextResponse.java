package com.mr.domain.playing.dto.res;

import com.fasterxml.jackson.databind.JsonNode;
import com.mr.domain.backingtrack.entity.BackingTrack;
import com.mr.domain.playing.entity.MidiEventData;
import com.mr.domain.playing.entity.Playing;
import com.mr.domain.playing.entity.enums.MidiType;

import java.time.Instant;
import java.util.List;

public record AnalysisContextResponse(
        Long playingId,
        String title,
        String genre,
        String key,
        Integer bpm,
        String timeSignature,
        Instant playedAt,
        Integer durationMinutes,
        Integer durationSec,
        String recordingFileUrl,
        String backingTrackAudioFileUrl,
        List<MidiEvent> midiEvents,
        JsonNode backingTrackMidiData,
        Integer totalBars
) {

    public static AnalysisContextResponse from(Playing playing, int totalBars, String recordingFileUrl) {
        BackingTrack backingTrack = playing.getBackingTrack();

        return new AnalysisContextResponse(
                playing.getId(),
                backingTrack.getTitle(),
                backingTrack.getGenre(),
                backingTrack.getKeySignature(),
                playing.getBpm(),
                backingTrack.getTimeSignature(),
                playing.getEndedAt(),
                toDurationMinutes(playing.getDurationSec()),
                playing.getDurationSec(),
                recordingFileUrl,
                backingTrack.getAudioFileUrl(),
                playing.getMidiData().stream().map(MidiEvent::from).toList(),
                backingTrack.getMidiData(),
                totalBars
        );
    }

    private static Integer toDurationMinutes(Integer durationSec) {
        return durationSec != null ? durationSec / 60 : null;
    }

    public record MidiEvent(
            Integer sequence,
            MidiType type,
            Integer pitch,
            Integer velocity,
            Long timestampMs
    ) {

        private static MidiEvent from(MidiEventData event) {
            return new MidiEvent(
                    event.getSequence(),
                    event.getType(),
                    event.getPitch(),
                    event.getVelocity(),
                    event.getTimestampMs()
            );
        }
    }
}
