package com.mr.domain.auth.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;

public record MasterAuthResponse(
        @Schema(description = "마스터 JWT 발급 대상 사용자 ID", example = "27")
        Long userId,

        @Schema(description = "API 인증에 사용하는 JWT Access Token", example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIyNyJ9.signature")
        String accessToken,

        @Schema(description = "Access Token 만료 시간(초)", example = "1800")
        Long accessTokenExpiresInSeconds
) {}
