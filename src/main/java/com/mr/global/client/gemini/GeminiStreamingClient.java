package com.mr.global.client.gemini;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mr.global.config.GeminiProperties;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class GeminiStreamingClient {

    private final RestClient geminiRestClient;
    private final GeminiProperties properties;
    private final ObjectMapper objectMapper;

    public String stream(String systemPrompt, String prompt, Consumer<String> chunkConsumer) {
        if (properties.apiKey() == null || properties.apiKey().isBlank()) {
            throw new IllegalStateException("Gemini API key is not configured.");
        }

        Map<String, Object> body = Map.of(
                "system_instruction", content(systemPrompt),
                "contents", List.of(Map.of(
                        "role", "user",
                        "parts", List.of(Map.of("text", prompt))
                )),
                "generationConfig", Map.of(
                        "temperature", 0.3,
                        "maxOutputTokens", 1_024
                )
        );

        return geminiRestClient.post()
                .uri("/v1beta/models/{model}:streamGenerateContent?alt=sse", properties.model())
                .header("x-goog-api-key", properties.apiKey())
                .body(body)
                .exchange((request, response) -> {
                    if (!response.getStatusCode().is2xxSuccessful()) {
                        throw new IllegalStateException(
                                "Gemini streaming request failed: " + response.getStatusCode());
                    }

                    StringBuilder answer = new StringBuilder();
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                            response.getBody(),
                            StandardCharsets.UTF_8
                    ))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (!line.startsWith("data:")) {
                                continue;
                            }
                            JsonNode event = objectMapper.readTree(line.substring(5).trim());
                            String chunk = extractText(event);
                            if (!chunk.isEmpty()) {
                                answer.append(chunk);
                                chunkConsumer.accept(chunk);
                            }
                        }
                    }

                    if (answer.isEmpty()) {
                        throw new IllegalStateException("Gemini returned an empty answer.");
                    }
                    return answer.toString();
                });
    }

    private Map<String, Object> content(String text) {
        return Map.of("parts", List.of(Map.of("text", text)));
    }

    private String extractText(JsonNode response) {
        JsonNode parts = response.path("candidates").path(0).path("content").path("parts");
        if (!parts.isArray()) {
            return "";
        }

        StringBuilder text = new StringBuilder();
        parts.forEach(part -> {
            if (part.path("text").isTextual()) {
                text.append(part.path("text").asText());
            }
        });
        return text.toString();
    }
}
