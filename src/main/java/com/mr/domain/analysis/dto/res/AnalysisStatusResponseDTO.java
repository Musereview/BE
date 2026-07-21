package com.mr.domain.analysis.dto.res;

import com.mr.domain.analysis.entity.enums.AnalysisStatus;
import java.time.LocalDateTime;

public record AnalysisStatusResponseDTO(
        Long analysisId,
        AnalysisStatus status,
        Integer progressRate,
        String message,
        LocalDateTime createdAt,
        LocalDateTime completedAt
) {
}