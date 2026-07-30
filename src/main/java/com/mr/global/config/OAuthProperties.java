package com.mr.global.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "oauth")
public record OAuthProperties(
        String frontendRedirectUri,
        ProviderProperties kakao,
        ProviderProperties google
) {
    public record ProviderProperties(
            String clientId,
            String clientSecret,
            String redirectUri,
            List<String> redirectUris
    ) {
        public List<String> getAllowedRedirectUris() {
            List<String> allowed = new ArrayList<>();
            if (redirectUris != null) {
                for (String uri : redirectUris) {
                    if (uri != null && !uri.isBlank()) {
                        allowed.add(uri.trim());
                    }
                }
            }
            if (redirectUri != null && !redirectUri.isBlank()) {
                String trimmed = redirectUri.trim();
                if (!allowed.contains(trimmed)) {
                    allowed.add(trimmed);
                }
            }
            return allowed;
        }
    }
}
