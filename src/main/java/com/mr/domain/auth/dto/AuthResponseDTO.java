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
            String email,
            String nickname,
            Boolean isNewUser, // 최초 가입 유저 여부 (회원가입/로그인 분기용)
            TokenResponse tokenInfo
    ) {}
}