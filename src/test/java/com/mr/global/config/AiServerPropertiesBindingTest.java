package com.mr.global.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class AiServerPropertiesBindingTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class);

    @EnableConfigurationProperties(AiServerProperties.class)
    static class TestConfig {
    }

    @Test
    void ai_internal_프리픽스로_설정값이_바인딩된다() {
        contextRunner
                .withPropertyValues(
                        "ai.internal.base-url=http://localhost:8000",
                        "ai.internal.connect-timeout=3s",
                        "ai.internal.read-timeout=30s",
                        "ai.internal.endpoints.analyze=/analyze"
                )
                .run(context -> {
                    AiServerProperties properties = context.getBean(AiServerProperties.class);
                    assertThat(properties.baseUrl()).isEqualTo("http://localhost:8000");
                    assertThat(properties.connectTimeout()).isEqualTo(Duration.ofSeconds(3));
                    assertThat(properties.readTimeout()).isEqualTo(Duration.ofSeconds(30));
                    assertThat(properties.endpoints().analyze()).isEqualTo("/analyze");
                });
    }

    @Test
    void 이전_ai_server_프리픽스로는_더이상_바인딩되지_않는다() {
        contextRunner
                .withPropertyValues(
                        "ai.server.base-url=http://old-prefix-should-be-ignored:9999",
                        "ai.internal.base-url=http://localhost:8000",
                        "ai.internal.endpoints.analyze=/analyze"
                )
                .run(context -> {
                    AiServerProperties properties = context.getBean(AiServerProperties.class);
                    assertThat(properties.baseUrl()).isEqualTo("http://localhost:8000");
                });
    }
}
