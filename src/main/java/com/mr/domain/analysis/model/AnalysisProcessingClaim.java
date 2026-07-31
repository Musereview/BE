package com.mr.domain.analysis.model;

import java.time.LocalDateTime;

public record AnalysisProcessingClaim(
        String requestJson,
        LocalDateTime processingStartedAt
) {
}
