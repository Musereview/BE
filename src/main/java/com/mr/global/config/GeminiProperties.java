package com.mr.global.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "external.ai")
public record GeminiProperties(
        String baseUrl,
        String apiKey,
        String model,
        Duration connectTimeout,
        Duration readTimeout
) {
    public GeminiProperties {
        baseUrl = hasText(baseUrl) ? baseUrl : "https://generativelanguage.googleapis.com";
        model = hasText(model) ? model : "gemini-3-flash-preview";
        connectTimeout = connectTimeout != null ? connectTimeout : Duration.ofSeconds(5);
        readTimeout = readTimeout != null ? readTimeout : Duration.ofSeconds(60);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
