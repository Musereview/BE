package com.mr.domain.analysis.dto.res;

import com.fasterxml.jackson.databind.JsonNode;
import com.mr.domain.analysis.entity.enums.AnalysisStatus;
import com.mr.domain.analysis.entity.enums.ContentFormat;
import com.mr.domain.analysis.entity.enums.LlmStatus;
import com.mr.domain.analysis.entity.enums.ReportGenerationType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

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
        BigDecimal totalScore,
        String grade,
        String summary,
        DomainScores domainScores,
        Report report,
        JsonNode result,
        LocalDateTime createdAt,
        LocalDateTime completedAt
) {

    public record DomainScores(
            BigDecimal scale,
            BigDecimal tension,
            BigDecimal progression,
            BigDecimal voiceLeading
    ) {
    }

    public record Report(
            Long analysisReportId,
            ReportGenerationType generationType,
            LlmStatus llmStatus,
            ContentFormat contentFormat,
            String content,
            String modelName,
            String promptVersion,
            LocalDateTime createdAt
    ) {
    }
}