package com.mr.domain.analysis.dto.res;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.mr.domain.analysis.entity.enums.AnalysisStatus;
import com.mr.domain.backingtrack.entity.BackingTrack;
import com.mr.domain.playing.entity.Playing;
import com.mr.domain.analysis.entity.Analysis;
import com.mr.domain.analysis.entity.AnalysisReport;
import com.mr.domain.analysis.entity.enums.AnalysisGrade;
import com.mr.domain.analysis.entity.enums.ContentFormat;
import com.mr.domain.analysis.entity.enums.LlmStatus;
import com.mr.domain.analysis.entity.enums.ReportGenerationType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;

public record AnalysisResultResponseDTO(
        Long analysisId,
        Long playingId,
        String title,
        String genre,
        String key,
        Integer bpm,

        @Schema(description = "연주 일시 (KST 기준 응답)", example = "2026-08-11T18:00:00", type = "string")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Asia/Seoul")
        Instant playedAt,

        String recordingFileUrl,
        String backingTrackAudioFileUrl,
        AnalysisStatus status,
        Integer startBar,
        Integer endBar,
        Integer totalScore,
        AnalysisGrade grade,
        String summary,
        DomainScores domainScores,
        Report report,
        @JsonProperty("result") JsonNode rawResult,

        @Schema(description = "생성 일시 (KST 기준 응답)", example = "2026-08-11T18:00:00", type = "string")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Asia/Seoul")
        Instant createdAt,

        @Schema(description = "완료 일시 (KST 기준 응답)", example = "2026-08-11T18:00:00", type = "string")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Asia/Seoul")
        Instant completedAt
) {

    public static AnalysisResultResponseDTO from(
            Analysis analysis,
            AnalysisReport analysisReport,
            JsonNode rawResult,
            String recordingFileUrl,
            String backingTrackAudioFileUrl
    ) {
        Playing playing = analysis.getPlaying();
        BackingTrack backingTrack = playing.getBackingTrack();
        return new AnalysisResultResponseDTO(
                analysis.getId(),
                playing.getId(),
                backingTrack != null ? backingTrack.getTitle() : null,
                backingTrack != null ? backingTrack.getGenre() : null,
                formatKey(backingTrack),
                playing.getBpm(),
                playing.getEndedAt(),
                recordingFileUrl,
                backingTrackAudioFileUrl,
                analysis.getStatus(),
                analysis.getStartBar(),
                analysis.getEndBar(),
                analysis.getTotalScore(),
                analysis.getGrade(),
                analysis.getSummary(),
                DomainScores.from(analysis),
                Report.fromNullable(analysisReport),
                rawResult,
                analysis.getCreatedAt(),
                analysis.getCompletedAt()
        );
    }

    private static String formatKey(BackingTrack backingTrack) {
        if (backingTrack == null || backingTrack.getKeySignature() == null || backingTrack.getScaleType() == null) {
            return null;
        }
        String scale = backingTrack.getScaleType().name().toLowerCase(Locale.ROOT);
        return backingTrack.getKeySignature()
                + " "
                + Character.toUpperCase(scale.charAt(0))
                + scale.substring(1);
    }

    public record DomainScores(
            @JsonProperty("scale") BigDecimal scaleScore,
            @JsonProperty("tension") BigDecimal tensionScore,
            @JsonProperty("progression") BigDecimal progressionScore,
            @JsonProperty("voiceLeading") BigDecimal voiceLeadingScore
    ) {

        private static DomainScores from(Analysis analysis) {
            return new DomainScores(
                    analysis.getScaleScore(),
                    analysis.getTensionScore(),
                    analysis.getProgressionScore(),
                    analysis.getVoiceLeadingScore()
            );
        }
    }

    public record Report(
            Long analysisReportId,
            ReportGenerationType generationType,
            LlmStatus llmStatus,
            ContentFormat contentFormat,
            String content,
            String modelName,
            String promptVersion,

            @Schema(description = "리포트 생성 일시 (KST 기준 응답)", example = "2026-08-11T18:00:00", type = "string")
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Asia/Seoul")
            Instant createdAt,

            @Schema(description = "리포트 수정 일시 (KST 기준 응답)", example = "2026-08-11T18:00:00", type = "string")
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Asia/Seoul")
            Instant updatedAt
    ) {

        private static Report fromNullable(AnalysisReport analysisReport) {
            if (analysisReport == null) {
                return null;
            }

            return new Report(
                    analysisReport.getId(),
                    analysisReport.getGenerationType(),
                    analysisReport.getLlmStatus(),
                    analysisReport.getContentFormat(),
                    analysisReport.getContent(),
                    analysisReport.getModelName(),
                    analysisReport.getPromptVersion(),
                    analysisReport.getCreatedAt(),
                    analysisReport.getUpdatedAt()
            );
        }
    }
}