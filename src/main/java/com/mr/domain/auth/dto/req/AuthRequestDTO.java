package com.mr.domain.auth.dto.req;

import com.mr.domain.auth.exception.AuthErrorStatus;
import com.mr.global.apipayload.exception.GeneralException;
import jakarta.validation.constraints.NotBlank;

import io.swagger.v3.oas.annotations.media.Schema;

public class AuthRequestDTO {

    @Schema(description = "소셜 Access Token 로그인/회원가입 요청 DTO")
    public record SocialLoginRequest(
            @Schema(description = "소셜 Access Token (카카오/구글 SDK 등에서 발급받은 액세스 토큰)", example = "sample_access_token")
            @NotBlank(message = "Access Token은 필수 입력값입니다.")
            String accessToken
    ) {}

    @Schema(description = "토큰 재발급 요청 DTO")
    public record TokenRefreshRequest(
            @Schema(description = "재발급에 사용할 Refresh Token", example = "sample_refresh_token_string")
            @NotBlank(message = "Refresh Token은 필수 입력값입니다.")
            String refreshToken
    ) {}

    @Schema(description = "로그아웃 요청 DTO")
    public record LogoutRequest(
            @Schema(description = "만료 처리할 기기의 Refresh Token", example = "sample_refresh_token_string")
            @NotBlank(message = "Refresh Token은 필수 입력값입니다.")
            String refreshToken
    ) {}

    @Schema(description = "임시 교환 코드를 통한 JWT 토큰 발급 요청 DTO")
    public record TokenExchangeRequest(
            @Schema(description = "소셜 로그인 콜백에서 전달받은 1회성 임시 코드", example = "sample_temp_exchange_code")
            @NotBlank(message = "임시 교환 코드는 필수 입력값입니다.")
            String code
    ) {}
}