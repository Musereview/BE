package com.mr.domain.auth.dto.res;

import lombok.Builder;

public class AuthResponseDTO {

    @Builder
    public record TokenResponse(
            String accessToken,
            String refreshToken,
            Long accessTokenExpiresInSeconds
    ) {}

    @Builder
    public record TokenInfo(
            String accessToken,
            String refreshToken,
            Long accessTokenExpiresInSeconds
    ) {}

    @Builder
    public record LoginResponse(
            Long userId,
            String nickname,
            boolean isNewUser,
            boolean isOnboardingCompleted,
            TokenResponse tokenInfo
    ) {}
}