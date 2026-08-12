package com.mr.domain.history.dto.res;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.JsonNode;
import com.mr.domain.analysis.entity.Analysis;
import com.mr.domain.analysis.entity.enums.AnalysisStatus;
import com.mr.domain.analysis.service.AnalysisBarCalculator.BarMetrics;
import com.mr.domain.backingtrack.entity.BackingTrack;
import com.mr.domain.playing.entity.MidiEventData;
import com.mr.domain.playing.entity.Playing;
import com.mr.domain.playing.entity.enums.MidiType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(description = "연주 히스토리 상세 조회 응답")
public record HistoryDetailResponseDTO(
        @Schema(description = "연주 기록 ID", example = "128")
        Long playingId,

        @Schema(description = "연주에 사용한 백킹트랙 ID. 백킹트랙이 없으면 null", example = "11")
        Long backingTrackId,

        @Schema(description = "연주에 사용한 백킹트랙 제목", example = "Autumn Leaves")
        String title,

        @Schema(description = "백킹트랙 장르", example = "JAZZ")
        String genre,

        @Schema(description = "백킹트랙 조성", example = "Bb")
        String key,

        @Schema(description = "연주 BPM", example = "120")
        Integer bpm,

        @Schema(description = "백킹트랙 박자", example = "4/4")
        String timeSignature,

        @Schema(description = "연주 종료 일시 (KST 기준 응답)", example = "2026-08-11T18:00:00", type = "string")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Asia/Seoul")
        Instant playedAt,

        @Schema(description = "연주 길이 (분 단위, 초 단위를 60으로 나눈 몫)", example = "3")
        Integer durationMinutes,

        @Schema(description = "연주 길이 (초 단위)", example = "215")
        Integer durationSec,

        @Schema(description = "녹음 파일 다운로드용 presigned URL")
        String recordingFileUrl,

        @Schema(description = "백킹트랙 음원 다운로드용 presigned URL. 음원이 없으면 null")
        String backingTrackAudioFileUrl,

        @Schema(description = "사용자 연주 MIDI 이벤트 목록")
        List<MidiEvent> midiEvents,

        @Schema(description = "백킹트랙 MIDI 데이터(JSON). 백킹트랙에 MIDI가 없으면 null")
        JsonNode backingTrackMidiData,

        @Schema(description = "연주 전체 마디 수. 백킹트랙 정보가 불완전하면 null", example = "32")
        Integer totalBars,

        @Schema(description = "해당 연주에 대한 분석 목록 (분석 시작 마디 오름차순)")
        List<AnalysisSummary> analyses
) {

    public static HistoryDetailResponseDTO from(
            Playing playing, List<Analysis> analyses, String recordingFileUrl,
            String backingTrackAudioFileUrl, BarMetrics barMetrics)
    {
        BackingTrack backingTrack = playing.getBackingTrack();

        return new HistoryDetailResponseDTO(
                playing.getId(),
                backingTrack != null ? backingTrack.getId() : null,
                backingTrack != null ? backingTrack.getTitle() : null,
                backingTrack != null ? backingTrack.getGenre() : null,
                backingTrack != null ? backingTrack.getKeySignature() : null,
                playing.getBpm(),
                backingTrack != null ? backingTrack.getTimeSignature() : null,
                playing.getEndedAt(),
                toDurationMinutes(playing.getDurationSec()),
                playing.getDurationSec(),
                recordingFileUrl,
                backingTrackAudioFileUrl,
                playing.getMidiData().stream().map(MidiEvent::from).toList(),
                backingTrack != null ? backingTrack.getMidiData() : null,
                barMetrics != null ? barMetrics.totalBars() : null,
                analyses.stream().map(analysis -> AnalysisSummary.from(analysis, barMetrics)).toList()
        );
    }

    private static Integer toDurationMinutes(Integer durationSec) {
        return durationSec != null ? durationSec / 60 : null;
    }

    @Schema(description = "연주 MIDI 이벤트")
    public record MidiEvent(
            @Schema(description = "이벤트 순번", example = "1")
            Integer sequence,

            @Schema(description = "이벤트 타입", example = "NOTE_ON")
            MidiType type,

            @Schema(description = "MIDI 음높이 (0~127)", example = "60")
            Integer pitch,

            @Schema(description = "타건 세기 (0~127)", example = "90")
            Integer velocity,

            @Schema(description = "연주 시작 기준 경과 시간 (ms)", example = "1200")
            Long timestampMs
    ) {

        public static MidiEvent from(MidiEventData event) {
            return new MidiEvent(
                    event.getSequence(),
                    event.getType(),
                    event.getPitch(),
                    event.getVelocity(),
                    event.getTimestampMs()
            );
        }
    }

    @Schema(description = "연주에 대한 분석 요약")
    public record AnalysisSummary(
            @Schema(description = "분석 ID", example = "342")
            Long analysisId,

            @Schema(description = "분석 구간 시작 마디", example = "1")
            Integer startBar,

            @Schema(description = "분석 구간 종료 마디", example = "8")
            Integer endBar,

            @Schema(description = "분석 구간으로 생성한 리포트 제목", example = "1마디-8마디 분석 리포트")
            String title,

            @Schema(description = "분석 한 줄 요약. 분석이 완료되기 전이면 null",
                    example = "코드 전환은 안정적이나 8마디 이후 박자가 밀립니다.")
            String oneLineSummary,

            @Schema(description = "분석 진행 상태", example = "COMPLETED")
            AnalysisStatus status,

            @Schema(description = "분석 구간의 재생 길이 (초). 백킹트랙 정보가 불완전하면 null", example = "16")
            Integer estimatedSeconds,

            @Schema(description = "분석 요청 일시 (KST 기준 응답)", example = "2026-08-11T18:00:00", type = "string")
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Asia/Seoul")
            Instant createdAt
    ) {

        public static AnalysisSummary from(Analysis analysis, BarMetrics barMetrics) {
            return new AnalysisSummary(
                    analysis.getId(),
                    analysis.getStartBar(),
                    analysis.getEndBar(),
                    analysis.getStartBar() + "마디-" + analysis.getEndBar() + "마디 분석 리포트",
                    analysis.getSummary(),
                    analysis.getStatus(),
                    toEstimatedSeconds(analysis, barMetrics),
                    analysis.getCreatedAt()
            );
        }

        // 분석 구간(startBar~endBar)의 재생 길이 산정
        private static Integer toEstimatedSeconds(Analysis analysis, BarMetrics barMetrics) {
            if (barMetrics == null) {
                return null;
            }

            int barCount = analysis.getEndBar() - analysis.getStartBar() + 1;
            return (int) Math.round(barCount * barMetrics.barDurationMs() / 1_000D);
        }
    }
}
