package com.mr.domain.auth.dto.req;

import com.mr.domain.auth.dto.OAuthCredential;
import com.mr.domain.auth.exception.AuthErrorStatus;
import com.mr.global.apipayload.exception.GeneralException;
import jakarta.validation.constraints.NotBlank;

public class AuthRequestDTO {

    public record SocialLoginRequest(
            String code,
            String authorizationCode,
            String accessToken,
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

    public record TokenRefreshRequest(
            @NotBlank(message = "Refresh Token은 필수 입력값입니다.")
            String refreshToken
    ) {}

    public record LogoutRequest(
            @NotBlank(message = "Refresh Token은 필수 입력값입니다.")
            String refreshToken
    ) {}
}