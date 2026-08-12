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
        @Schema(description = "연주 ID", example = "128")
        Long playingId,

        @Schema(description = "재연주에 사용할 백킹트랙 ID", example = "11")
        Long backingTrackId,

        @Schema(description = "백킹트랙 제목", example = "Autumn Leaves")
        String title,

        @Schema(description = "장르", example = "JAZZ")
        String genre,

        @Schema(description = "조성", example = "Bb")
        String key,

        @Schema(description = "연주 BPM", example = "120")
        Integer bpm,

        @Schema(description = "박자표", example = "4/4")
        String timeSignature,


        @Schema(description = "연주 일시 (KST 기준 응답)", example = "2026-08-11T18:00:00", type = "string")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Asia/Seoul")
        Instant playedAt,

        @Schema(description = "연주 시간(분)", example = "3")
        Integer durationMinutes,

        @Schema(description = "연주 시간(초)", example = "185")
        Integer durationSec,

        @Schema(
                description = "연주 녹음 파일 접근 URL",
                example = "https://example.com/recording.webm"
        )
        String recordingFileUrl,

        @Schema(
                description = "백킹트랙 오디오 파일 접근 URL",
                example = "https://example.com/backing-track.mp3"
        )
        String backingTrackAudioFileUrl,

        @Schema(description = "연주 중 수집된 MIDI 이벤트 목록")
        List<MidiEvent> midiEvents,

        @Schema(description = "백킹트랙 MIDI 데이터")
        JsonNode backingTrackMidiData,

        @Schema(description = "분석 가능한 전체 마디 수", example = "32")
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

            @Schema(description = "MIDI 이벤트 순서", example = "0")
            Integer sequence,

            @Schema(description = "MIDI 이벤트 타입", example = "NOTE_ON")
            MidiType type,

            @Schema(description = "MIDI 피치 값", example = "60")
            Integer pitch,

            @Schema(description = "MIDI 입력 강도", example = "100")
            Integer velocity,

            @Schema(description = "연주 시작 기준 MIDI 이벤트 발생 시간(ms)", example = "1250")
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
