package com.mr.domain.analysis.model;

import java.time.Instant;

public record AnalysisProcessingClaim(
        String requestJson,
        Instant processingStartedAt
) {
}
