package com.mr.domain.analysis.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.mr.domain.mentor.entity.enums.LlmCallStatus;
import java.math.BigDecimal;

public record LlmCallMetadata(
        LlmCallStatus status,
        String modelName,
        String promptVersion,
        JsonNode promptSnapshot,
        Integer promptTokens,
        Integer completionTokens,
        Integer totalTokens,
        BigDecimal temperature,
        Integer latencyMs,
        boolean cacheHit,
        String inputHash,
        String errorMessage
) {
}
