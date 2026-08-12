package com.mr.domain.playing.dto.res;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.JsonNode;
import com.mr.domain.backingtrack.entity.BackingTrack;
import com.mr.domain.playing.entity.MidiEventData;
import com.mr.domain.playing.entity.Playing;
import com.mr.domain.playing.entity.enums.MidiType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

public record AnalysisContextResponse(
        Long playingId,
        @Schema(description = "재연주에 사용할 백킹트랙 ID", example = "11") Long backingTrackId,
        String title,
        String genre,
        String key,
        Integer bpm,
        String timeSignature,

        @Schema(description = "연주 일시 (KST 기준 응답)", example = "2026-08-11T18:00:00", type = "string")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Asia/Seoul")
        Instant playedAt,

        Integer durationMinutes,
        Integer durationSec,
        String recordingFileUrl,
        String backingTrackAudioFileUrl,
        List<MidiEvent> midiEvents,
        JsonNode backingTrackMidiData,
        Integer totalBars
) {

    public static AnalysisContextResponse from(
            Playing playing, int totalBars, String recordingFileUrl, String backingTrackAudioFileUrl)
    {
        BackingTrack backingTrack = playing.getBackingTrack();

        return new AnalysisContextResponse(
                playing.getId(),
                backingTrack.getId(),
                backingTrack.getTitle(),
                backingTrack.getGenre(),
                backingTrack.getKeySignature(),
                playing.getBpm(),
                backingTrack.getTimeSignature(),
                playing.getEndedAt(),
                toDurationMinutes(playing.getDurationSec()),
                playing.getDurationSec(),
                recordingFileUrl,
                backingTrackAudioFileUrl,
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
