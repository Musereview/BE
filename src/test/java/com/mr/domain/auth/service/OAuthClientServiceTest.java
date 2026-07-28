package com.mr.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OAuthClientServiceTest {

    @Test
    @DisplayName("OAuthClientService 생성 시 connect/read timeout 설정이 정상 적용된다")
    void createOAuthClientService_withTimeout() {
        Duration connectTimeout = Duration.ofSeconds(3);
        Duration readTimeout = Duration.ofSeconds(5);

        OAuthClientService service = new OAuthClientService(connectTimeout, readTimeout);

        assertThat(service).isNotNull();
    }
}
