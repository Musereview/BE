package com.mr.domain.playing.dto.res;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.mr.domain.backingtrack.entity.BackingTrack;
import com.mr.domain.backingtrack.entity.ChordProgression;
import com.mr.domain.backingtrack.entity.enums.ScaleType;
import com.mr.domain.playing.entity.Playing;
import com.mr.domain.playing.entity.enums.PlayingStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

public record PlayingStartResponse(

        @Schema(description = "생성된 연주 ID", example = "128")
        Long playingId,

        @Schema(description = "연주 상태", example = "IN_PROGRESS")
        PlayingStatus status,

        @Schema(description = "연주에 사용되는 백킹트랙 정보")
        BackingTrackResponse backingTrack,

        @Schema(description = "시작 일시 (KST 기준 응답)", example = "2026-08-11T18:00:00", type = "string")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Asia/Seoul")
        Instant startedAt
) {

    public static PlayingStartResponse from (Playing playing, String backingTrackAudioFileUrl) {
        return new PlayingStartResponse(
                playing.getId(),
                playing.getStatus(),
                BackingTrackResponse.from(
                        playing.getBackingTrack(), backingTrackAudioFileUrl),
                playing.getStartedAt()
        );
    }

    public record BackingTrackResponse(

            @Schema(description = "백킹트랙 ID", example = "25")
            Long backingTrackId,

            @Schema(description = "백킹트랙 제목", example = "Summer")
            String title,

            @Schema(
                    description = "백킹트랙 오디오 파일 접근 URL",
                    example = "https://example.com/backing-track.mp3"
            )
            String audioFileUrl,

            @Schema(description = "장르", example = "JAZZ")
            String genre,

            @Schema(description = "조성", example = "Bb")
            String keySignature,

            @Schema(description = "스케일 타입", example = "MAJOR")
            ScaleType scaleType,

            @Schema(description = "BPM", example = "120")
            Integer bpm,

            @Schema(description = "박자표", example = "4/4")
            String timeSignature,

            @Schema(description = "백킹트랙 재생 시간(초)", example = "180")
            Integer playtimeSec,

            @Schema(description = "코드 진행 목록")
            List<ChordProgressionResponse> chordProgression

    ) {
        public static BackingTrackResponse from(BackingTrack backingTrack, String audioFileUrl) {
            return new BackingTrackResponse(
                    backingTrack.getId(),
                    backingTrack.getTitle(),
                    audioFileUrl,
                    backingTrack.getGenre(),
                    backingTrack.getKeySignature(),
                    backingTrack.getScaleType(),
                    backingTrack.getBpm(),
                    backingTrack.getTimeSignature(),
                    backingTrack.getPlaytimeSec(),
                    backingTrack.getChordProgressions()
                            .stream()
                            .map(ChordProgressionResponse::from)
                            .toList()
            );
        }
    }

    public record ChordProgressionResponse(

            @Schema(description = "마디 번호", example = "1")
            Integer measureNo,

            @Schema(description = "마디 내 코드 순서", example = "1")
            Integer sequenceNo,

            @Schema(description = "코드명", example = "Cm7")
            String chordName
    ) {
        public static ChordProgressionResponse from(ChordProgression chordProgression) {

            return new ChordProgressionResponse(
                    chordProgression.getMeasureNo(),
                    chordProgression.getSequenceNo(),
                    chordProgression.getChordName()
            );
        }
    }
}