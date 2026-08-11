package com.mr.domain.analysis.dto.res;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.mr.domain.analysis.entity.Analysis;
import com.mr.domain.analysis.entity.enums.AnalysisStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public record AnalysisCreateResponseDTO(
        Long analysisId,
        Long playingId,
        AnalysisStatus status,
        Integer startBar,
        Integer endBar,
        @Schema(description = "분석 생성 일시 (KST 기준 응답)", example = "2026-08-11T18:00:00", type = "string")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Asia/Seoul")
        Instant createdAt
) {
    public static AnalysisCreateResponseDTO from(Analysis analysis) {
        return new AnalysisCreateResponseDTO(analysis.getId(), analysis.getPlaying().getId(), analysis.getStatus(),
                analysis.getStartBar(), analysis.getEndBar(), analysis.getCreatedAt());
    }
}
