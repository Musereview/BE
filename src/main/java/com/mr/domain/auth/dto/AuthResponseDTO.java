package com.mr.domain.auth.dto;

import lombok.Builder;

public class AuthResponseDTO {

    @Builder
    public record TokenResponse(
            String accessToken,
            String refreshToken,
            Long accessTokenExpiresInSeconds
    ) {}

    @Builder
    public record LoginResponse(
            Long userId,
            String nickname,
            boolean isNewUser,
            TokenResponse tokenInfo
    ) {}
}