package com.mr.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;

public class AuthRequestDTO {

    // 프론트엔드에서 소셜 인가 코드로 받아온 AccessToken 제출용
    public record SocialLoginRequest(
            @NotBlank(message = "소셜 액세스 토큰은 필수 입력값입니다.")
            String accessToken
    ) {}

    // Refresh Token 갱신 요청용
    public record TokenRefreshRequest(
            @NotBlank(message = "Refresh Token은 필수 입력값입니다.")
            String refreshToken
    ) {}
}