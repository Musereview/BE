package com.mr.domain.analysis.dto.res;

import com.fasterxml.jackson.databind.JsonNode;
import com.mr.domain.analysis.entity.Analysis;
import com.mr.domain.analysis.entity.AnalysisReport;
import com.mr.domain.analysis.entity.enums.AnalysisGrade;
import com.mr.domain.analysis.entity.enums.ContentFormat;
import com.mr.domain.analysis.entity.enums.LlmStatus;
import com.mr.domain.analysis.entity.enums.ReportGenerationType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AnalysisResultResponseDTO(
        Long analysisId,
        Long playingId,
        Integer startBar,
        Integer endBar,
        Integer totalScore,
        AnalysisGrade grade,
        String summary,
        DomainScores domainScores,
        Report report,
        JsonNode rawResult,
        LocalDateTime createdAt,
        LocalDateTime completedAt
) {

    public static AnalysisResultResponseDTO from(
            Analysis analysis,
            AnalysisReport analysisReport,
            JsonNode rawResult
    ) {
        return new AnalysisResultResponseDTO(
                analysis.getId(),
                analysis.getPlayingId(),
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

    public record DomainScores(
            BigDecimal scaleScore,
            BigDecimal tensionScore,
            BigDecimal progressionScore,
            BigDecimal voiceLeadingScore
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