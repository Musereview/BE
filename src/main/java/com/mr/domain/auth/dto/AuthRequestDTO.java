package com.mr.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;

public class AuthRequestDTO {

    public record SocialLoginRequest(
            @NotBlank(message = "소셜 액세스 토큰은 필수 입력값입니다.")
            String accessToken
    ) {}

    public record TokenRefreshRequest(
            @NotBlank(message = "Refresh Token은 필수 입력값입니다.")
            String refreshToken
    ) {}
}