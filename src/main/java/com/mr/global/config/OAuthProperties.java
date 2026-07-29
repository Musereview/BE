package com.mr.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "oauth")
public record OAuthProperties(
        ProviderProperties kakao,
        ProviderProperties google
) {
    public record ProviderProperties(
            String clientId,
            String clientSecret,
            String redirectUri
    ) {}
}
