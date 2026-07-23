package com.mr.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "ai.internal")
public record AiServerProperties(
        String baseUrl,
        Duration connectTimeout,
        Duration readTimeout,
        Endpoints endpoints
) {
    public record Endpoints(
            String analyze
    ) {
    }
}
