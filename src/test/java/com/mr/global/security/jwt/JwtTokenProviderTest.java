package com.mr.global.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.mr.global.security.principal.CustomUserDetailsService;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

    private static final String SECRET = Base64.getEncoder()
            .encodeToString("01234567890123456789012345678901".getBytes(StandardCharsets.UTF_8));

    @Test
    void accessTokenValidation_distinguishesFailureReasons() {
        JwtTokenProvider provider = createProvider(60L, 120L);
        String accessToken = provider.createAccessToken(1L);
        String refreshToken = provider.createRefreshToken(1L);

        assertThat(provider.validateAccessTokenResult(accessToken)).isEqualTo(JwtValidationResult.VALID);
        assertThat(provider.validateAccessTokenResult(refreshToken)).isEqualTo(JwtValidationResult.INVALID_TYPE);
        assertThat(provider.validateAccessTokenResult("not-a-jwt")).isEqualTo(JwtValidationResult.MALFORMED);
        assertThat(provider.validateAccessTokenResult(tamperSignature(accessToken)))
                .isEqualTo(JwtValidationResult.INVALID_SIGNATURE);
    }

    @Test
    void expiredTokens_areRejectedByExistingBooleanValidationMethods() {
        JwtTokenProvider provider = createProvider(-1L, -1L);
        String accessToken = provider.createAccessToken(1L);
        String refreshToken = provider.createRefreshToken(1L);

        assertThat(provider.validateAccessTokenResult(accessToken)).isEqualTo(JwtValidationResult.EXPIRED);
        assertThat(provider.validateRefreshTokenResult(refreshToken)).isEqualTo(JwtValidationResult.EXPIRED);
        assertThat(provider.validateAccessToken(accessToken)).isFalse();
        assertThat(provider.validateRefreshToken(refreshToken)).isFalse();
    }

    @Test
    void accessAndRefreshTokenValidation_preservesTokenTypeBehavior() {
        JwtTokenProvider provider = createProvider(60L, 120L);
        String accessToken = provider.createAccessToken(1L);
        String refreshToken = provider.createRefreshToken(1L);

        assertThat(provider.validateAccessToken(accessToken)).isTrue();
        assertThat(provider.validateRefreshToken(refreshToken)).isTrue();
        assertThat(provider.validateAccessToken(refreshToken)).isFalse();
        assertThat(provider.validateRefreshToken(accessToken)).isFalse();
    }

    private JwtTokenProvider createProvider(long accessValidity, long refreshValidity) {
        JwtProperties properties = new JwtProperties(SECRET, accessValidity, refreshValidity);
        JwtTokenProvider provider = new JwtTokenProvider(mock(CustomUserDetailsService.class), properties);
        provider.init();
        return provider;
    }

    private String tamperSignature(String token) {
        String[] segments = token.split("[.]");
        byte[] signature = Base64.getUrlDecoder().decode(segments[2]);
        signature[0] ^= 0x01;
        segments[2] = Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
        return String.join(".", segments);
    }
}
