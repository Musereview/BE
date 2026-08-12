package com.mr.global.client.gemini;

import com.fasterxml.jackson.databind.JsonNode;
import com.mr.global.config.GeminiProperties;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class GeminiClient {

    private static final Map<String, Object> REPORT_RESPONSE_SCHEMA = Map.of(
            "type", "OBJECT",
            "properties", Map.of(
                    "summary", Map.of(
                            "type", "STRING",
                            "description", "연주의 핵심 강점과 보완점을 담은 한 문장 요약"
                    ),
                    "report", Map.of(
                            "type", "STRING",
                            "description", "정해진 섹션 구조를 따르는 한국어 Markdown 리포트"
                    )
            ),
            "required", List.of("summary", "report")
    );

    private final RestClient geminiRestClient;
    private final GeminiProperties properties;

    public GeminiGenerationResult generateReport(String systemPrompt, String analysisJson) {
        if (properties.apiKey() == null || properties.apiKey().isBlank()) {
            throw new IllegalStateException("Gemini API key is not configured.");
        }

        Map<String, Object> body = Map.of(
                "system_instruction", content(systemPrompt),
                "contents", List.of(Map.of(
                        "role", "user",
                        "parts", List.of(Map.of("text", analysisJson))
                )),
                "generationConfig", Map.of(
                        "temperature", 0.3,
                        "maxOutputTokens", 4_096,
                        "responseMimeType", "application/json",
                        "responseSchema", REPORT_RESPONSE_SCHEMA
                )
        );

        JsonNode response = geminiRestClient.post()
                .uri("/v1beta/models/{model}:generateContent", properties.model())
                .header("x-goog-api-key", properties.apiKey())
                .body(body)
                .retrieve()
                .body(JsonNode.class);

        String content = extractText(response);
        if (content == null || content.isBlank()) {
            throw new IllegalStateException("Gemini returned an empty report.");
        }
        JsonNode usage = response.path("usageMetadata");
        return new GeminiGenerationResult(
                content.trim(),
                integerOrNull(usage.path("promptTokenCount")),
                integerOrNull(usage.path("candidatesTokenCount")),
                integerOrNull(usage.path("totalTokenCount")),
                usage.path("cachedContentTokenCount").asInt(0) > 0
        );
    }

    private Map<String, Object> content(String text) {
        return Map.of("parts", List.of(Map.of("text", text)));
    }

    private String extractText(JsonNode response) {
        if (response == null) {
            return null;
        }
        JsonNode parts = response.path("candidates").path(0).path("content").path("parts");
        if (!parts.isArray()) {
            return null;
        }
        StringBuilder text = new StringBuilder();
        for (JsonNode part : parts) {
            if (part.path("text").isTextual()) {
                text.append(part.path("text").asText());
            }
        }
        return text.toString();
    }

    private Integer integerOrNull(JsonNode node) {
        return node.canConvertToInt() ? node.intValue() : null;
    }
}
