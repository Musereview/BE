package com.mr.domain.auth.dto;

import lombok.Builder;

public class AuthResponseDTO {

    @Builder
    public record TokenResponse(
            String accessToken,
            String refreshToken,
            Long tokenExpirationTime
    ) {}

    @Builder
    public record LoginResponse(
            Long userId,
            String nickname,
            Boolean isNewUser, // 최초 가입 여부
            TokenResponse tokenInfo
    ) {}
}