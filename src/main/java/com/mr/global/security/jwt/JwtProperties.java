package com.mr.global.security.jwt;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        @NotBlank(message = "JWT Secret Key는 필수 입력값입니다.")
        String secret,

        @Positive(message = "Access Token 만료 시간은 양수여야 합니다.")
        long accessTokenValidityInSeconds,

        @Positive(message = "Refresh Token 만료 시간은 양수여야 합니다.")
        long refreshTokenValidityInSeconds
) {
}