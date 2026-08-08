package com.mr.domain.history.dto.res;

import com.fasterxml.jackson.databind.JsonNode;
import com.mr.domain.analysis.entity.Analysis;
import com.mr.domain.analysis.entity.enums.AnalysisStatus;
import com.mr.domain.backingtrack.entity.BackingTrack;
import com.mr.domain.playing.entity.MidiEventData;
import com.mr.domain.playing.entity.Playing;
import com.mr.domain.playing.entity.enums.MidiType;

import java.time.LocalDateTime;
import java.util.List;

public record HistoryDetailResponseDTO(
        Long playingId,
        String title,
        String genre,
        String key,
        Integer bpm,
        String timeSignature,
        LocalDateTime playedAt,
        Integer durationMinutes,
        Integer durationSec,
        String recordingFileUrl,
        String backingTrackAudioFileUrl,
        List<MidiEvent> midiEvents,
        JsonNode backingTrackMidiData,
        Integer totalBars,
        List<AnalysisSummary> analyses
) {

    public static HistoryDetailResponseDTO from(Playing playing, List<Analysis> analyses, String recordingFileUrl) {
        BackingTrack backingTrack = playing.getBackingTrack();

        return new HistoryDetailResponseDTO(
                playing.getId(),
                backingTrack != null ? backingTrack.getTitle() : null,
                backingTrack != null ? backingTrack.getGenre() : null,
                backingTrack != null ? backingTrack.getKeySignature() : null,
                playing.getBpm(),
                backingTrack != null ? backingTrack.getTimeSignature() : null,
                playing.getEndedAt(),
                toDurationMinutes(playing.getDurationSec()),
                playing.getDurationSec(),
                recordingFileUrl,
                backingTrack != null ? backingTrack.getAudioFileUrl() : null,
                playing.getMidiData().stream().map(MidiEvent::from).toList(),
                backingTrack != null ? backingTrack.getMidiData() : null,
                null, // TODO: totalBars - playtimeSec/bpm 기반 추정 가능, 정확도 논의 후 추가
                analyses.stream().map(AnalysisSummary::from).toList()
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

    public record AnalysisSummary(
            Long analysisId,
            Integer startBar,
            Integer endBar,
            String title,
            String oneLineSummary,
            AnalysisStatus status,
            Integer estimatedSeconds,
            LocalDateTime createdAt
    ) {

        public static AnalysisSummary from(Analysis analysis) {
            return new AnalysisSummary(
                    analysis.getId(),
                    analysis.getStartBar(),
                    analysis.getEndBar(),
                    analysis.getStartBar() + "마디-" + analysis.getEndBar() + "마디 분석 리포트",
                    analysis.getSummary(),
                    analysis.getStatus(),
                    null, // TODO: estimatedSeconds 소스 없음
                    analysis.getCreatedAt()
            );
        }
    }
}
