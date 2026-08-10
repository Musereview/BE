package com.mr.domain.analysis.dto.res;

import com.mr.domain.analysis.entity.Analysis;
import com.mr.domain.analysis.entity.enums.AnalysisStatus;

import java.time.Instant;

public record AnalysisStatusResponseDTO(
        Long analysisId,
        AnalysisStatus status,
        Integer progressRate,
        String message,
        Instant createdAt,
        Instant completedAt
) {

    public static AnalysisStatusResponseDTO from(
            Analysis analysis,
            Integer progressRate,
            String message
    ) {
        return new AnalysisStatusResponseDTO(
                analysis.getId(),
                analysis.getStatus(),
                progressRate,
                message,
                analysis.getCreatedAt(),
                analysis.getCompletedAt()
        );
    }
}