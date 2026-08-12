package com.mr.domain.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.mr.domain.auth.service.MasterAuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class MasterAuthActivationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(MasterAuthService.class, () -> mock(MasterAuthService.class))
            .withUserConfiguration(MasterAuthController.class);

    @Test
    @DisplayName("설정이 없으면 마스터 인증 컨트롤러를 등록하지 않는다")
    void masterAuth_missingProperty_isDisabled() {
        contextRunner.run(context -> assertThat(context).doesNotHaveBean(MasterAuthController.class));
    }

    @Test
    @DisplayName("기능 활성화 시 설정된 사용자 ID로 컨트롤러를 등록한다")
    void masterAuth_enabledWithUserId_isEnabled() {
        contextRunner
                .withPropertyValues("master-auth.enabled=true", "master-auth.user-id=27")
                .run(context -> assertThat(context).hasSingleBean(MasterAuthController.class));
    }

    @Test
    @DisplayName("기능 활성화 시 사용자 ID가 없으면 애플리케이션 컨텍스트 생성에 실패한다")
    void masterAuth_enabledWithoutUserId_failsFast() {
        contextRunner
                .withPropertyValues("master-auth.enabled=true")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    @DisplayName("기능 활성화 시 사용자 ID가 양수가 아니면 애플리케이션 컨텍스트 생성에 실패한다")
    void masterAuth_enabledWithInvalidUserId_failsFast() {
        contextRunner
                .withPropertyValues("master-auth.enabled=true", "master-auth.user-id=0")
                .run(context -> assertThat(context).hasFailed());
    }
}
