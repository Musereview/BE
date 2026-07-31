package com.mr.domain.analysis.dto.res;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.mr.domain.analysis.entity.enums.AnalysisStatus;
import com.mr.domain.backingtrack.entity.BackingTrack;
import com.mr.domain.playing.entity.Playing;
import com.mr.domain.analysis.entity.Analysis;
import com.mr.domain.analysis.entity.AnalysisReport;
import com.mr.domain.analysis.entity.enums.AnalysisGrade;
import com.mr.domain.analysis.entity.enums.ContentFormat;
import com.mr.domain.analysis.entity.enums.LlmStatus;
import com.mr.domain.analysis.entity.enums.ReportGenerationType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;

public record AnalysisResultResponseDTO(
        Long analysisId,
        Long playingId,
        String title,
        String genre,
        String key,
        Integer bpm,
        LocalDateTime playedAt,
        AnalysisStatus status,
        Integer startBar,
        Integer endBar,
        Integer totalScore,
        AnalysisGrade grade,
        String summary,
        DomainScores domainScores,
        Report report,
        @JsonProperty("result") JsonNode rawResult,
        LocalDateTime createdAt,
        LocalDateTime completedAt
) {

    public static AnalysisResultResponseDTO from(
            Analysis analysis,
            AnalysisReport analysisReport,
            JsonNode rawResult
    ) {
        Playing playing = analysis.getPlaying();
        BackingTrack backingTrack = playing.getBackingTrack();
        return new AnalysisResultResponseDTO(
                analysis.getId(),
                playing.getId(),
                backingTrack.getTitle(),
                backingTrack.getGenre(),
                formatKey(backingTrack),
                playing.getBpm(),
                playing.getEndedAt(),
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
            LocalDateTime createdAt,
            LocalDateTime updatedAt
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
