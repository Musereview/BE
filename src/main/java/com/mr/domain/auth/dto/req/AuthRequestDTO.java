package com.mr.domain.auth.dto.req;

import com.mr.domain.auth.dto.OAuthCredential;
import com.mr.domain.auth.exception.AuthErrorStatus;
import com.mr.global.apipayload.exception.GeneralException;
import jakarta.validation.constraints.NotBlank;

import io.swagger.v3.oas.annotations.media.Schema;

public class AuthRequestDTO {

    @Schema(description = "소셜 로그인 요청 DTO (code, authorizationCode, accessToken 중 하나를 전달하며, redirectUri는 생략 가능)")
    public record SocialLoginRequest(
            @Schema(description = "소셜 인가 코드 (authorizationCode와 동일 역할, 호환용)", example = "sample_authorization_code")
            String code,

            @Schema(description = "소셜 인가 코드 풀네임 (code와 동일 역할, 호환용)", example = "sample_authorization_code")
            String authorizationCode,

            @Schema(description = "소셜 Access Token (인가 코드 대신 직접 액세스 토큰 전송 시 사용)", example = "sample_access_token")
            String accessToken,

            @Schema(description = "커스텀 Redirect URI (생략 시 백엔드 기본값 자동 적용)", example = "http://localhost:8080/api/auth/kakao/callback")
            String redirectUri
    ) {
        /**
         * code, authorizationCode, accessToken 중 전송된 값으로 OAuthCredential을 생성합니다.
         * 필수 입력값 누락 검증도 본 메서드에서 수행합니다.
         */
        public OAuthCredential getCredential() {
            if (code != null && !code.isBlank()) {
                return new OAuthCredential(OAuthCredential.CredentialType.AUTHORIZATION_CODE, code.trim());
            }
            if (authorizationCode != null && !authorizationCode.isBlank()) {
                return new OAuthCredential(OAuthCredential.CredentialType.AUTHORIZATION_CODE, authorizationCode.trim());
            }
            if (accessToken != null && !accessToken.isBlank()) {
                return new OAuthCredential(OAuthCredential.CredentialType.ACCESS_TOKEN, accessToken.trim());
            }
            throw new GeneralException(AuthErrorStatus.INVALID_AUTH_REQUEST);
        }
    }

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
}