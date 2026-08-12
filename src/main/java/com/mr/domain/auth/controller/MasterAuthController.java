package com.mr.domain.auth.controller;

import com.mr.domain.auth.dto.res.MasterAuthResponse;
import com.mr.domain.auth.service.MasterAuthService;
import com.mr.global.apipayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@ConditionalOnProperty(prefix = "master-auth", name = "enabled", havingValue = "true")
@Tag(name = "인증", description = "인증 API")
public class MasterAuthController {

    private final MasterAuthService masterAuthService;
    private final Long masterAuthUserId;

    public MasterAuthController(
            MasterAuthService masterAuthService,
            @Value("${master-auth.user-id}") Long masterAuthUserId
    ) {
        if (masterAuthUserId == null || masterAuthUserId <= 0) {
            throw new IllegalArgumentException("master-auth.user-id는 양수여야 합니다.");
        }
        this.masterAuthService = masterAuthService;
        this.masterAuthUserId = masterAuthUserId;
    }

    @SecurityRequirements
    @Operation(
            summary = "마스터 JWT 발급 API",
            description = "API 테스트용으로 설정된 사용자의 JWT Access Token을 발급합니다."
    )
    @PostMapping("/master-token")
    public ApiResponse<MasterAuthResponse> issueAccessToken() {
        return ApiResponse.onSuccess(masterAuthService.issueAccessToken(masterAuthUserId));
    }
}
