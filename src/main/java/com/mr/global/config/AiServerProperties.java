package com.mr.global.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "ai.internal")
public record AiServerProperties(
        @NotBlank String baseUrl,
        Duration connectTimeout,
        Duration readTimeout,
        Endpoints endpoints
) {
    public record Endpoints(
            String analyze
    ) {
    }
}
