package com.mr.domain.analysis.dto.res;

import com.mr.domain.analysis.entity.Analysis;
import com.mr.domain.analysis.entity.enums.AnalysisStatus;

import java.time.Instant;

public record AnalysisCreateResponseDTO(
        Long analysisId,
        Long playingId,
        AnalysisStatus status,
        Integer startBar,
        Integer endBar,
        Instant createdAt
) {
    public static AnalysisCreateResponseDTO from(Analysis analysis) {
        return new AnalysisCreateResponseDTO(analysis.getId(), analysis.getPlaying().getId(), analysis.getStatus(),
                analysis.getStartBar(), analysis.getEndBar(), analysis.getCreatedAt());
    }
}
