package com.mr.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "external.ai")
public record AiServerProperties(
        String baseUrl,
        String apiKey,
        String model,
        Duration connectTimeout,
        Duration readTimeout,
        Endpoints endpoints
) {
    public record Endpoints(
            String analyze,
            String report
    ) {
    }
}
