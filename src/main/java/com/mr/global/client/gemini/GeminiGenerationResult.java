package com.mr.global.client.gemini;

public record GeminiGenerationResult(
        String content,
        Integer promptTokens,
        Integer completionTokens,
        Integer totalTokens,
        boolean cacheHit
) {
}
