package com.mr.domain.auth.dto;

import com.mr.domain.auth.exception.AuthErrorStatus;
import com.mr.global.apipayload.exception.GeneralException;

public class AuthRequestDTO {

    public record SocialLoginRequest(
            String code,
            String authorizationCode,
            String accessToken,
            String redirectUri
    ) {
        public String getEffectiveCodeOrToken() {
            if (code != null && !code.isBlank()) {
                return code.trim();
            }
            if (authorizationCode != null && !authorizationCode.isBlank()) {
                return authorizationCode.trim();
            }
            if (accessToken != null && !accessToken.isBlank()) {
                return accessToken.trim();
            }
            throw new GeneralException(AuthErrorStatus.INVALID_AUTH_REQUEST);
        }
    }

    public record TokenRefreshRequest(
            String refreshToken
    ) {}
}