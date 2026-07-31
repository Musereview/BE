package com.mr.domain.auth.service;

import com.mr.domain.auth.dto.OAuthUserInfo;
import com.mr.domain.auth.dto.res.GoogleUserResponse;
import com.mr.domain.auth.dto.res.KakaoUserResponse;
import com.mr.domain.auth.dto.res.OAuthTokenResponse;
import com.mr.domain.auth.entity.enums.SocialType;
import com.mr.domain.auth.exception.AuthErrorStatus;
import com.mr.domain.auth.exception.OAuthExceptionMapper;
import com.mr.global.apipayload.exception.GeneralException;
import com.mr.global.config.OAuthProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import java.util.List;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
public class OAuthClientService {

    private final RestClient restClient;
    private final OAuthExceptionMapper exceptionMapper;
    private final OAuthProperties oAuthProperties;

    public OAuthClientService(
            @Qualifier("oauthRestClient") RestClient restClient,
            OAuthExceptionMapper exceptionMapper,
            OAuthProperties oAuthProperties) {
        this.restClient = restClient;
        this.exceptionMapper = exceptionMapper;
        this.oAuthProperties = oAuthProperties;
    }

    public OAuthUserInfo getUserInfo(SocialType socialType, String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new GeneralException(AuthErrorStatus.INVALID_AUTH_REQUEST);
        }
        String cleanToken = accessToken.trim();
        if (cleanToken.toLowerCase().startsWith("bearer ")) {
            cleanToken = cleanToken.substring(7).trim();
        }
        if (cleanToken.isBlank()) {
            throw new GeneralException(AuthErrorStatus.INVALID_AUTH_REQUEST);
        }

        return switch (socialType) {
            case KAKAO -> fetchKakaoUserInfo(cleanToken);
            case GOOGLE -> fetchGoogleUserInfo(cleanToken);
        };
    }

    public OAuthUserInfo getUserInfoByCode(SocialType socialType, String code, String customRedirectUri) {
        String accessToken = switch (socialType) {
            case KAKAO -> exchangeKakaoCode(code, customRedirectUri);
            case GOOGLE -> exchangeGoogleCode(code, customRedirectUri);
        };
        return getUserInfo(socialType, accessToken);
    }

    private String exchangeKakaoCode(String code, String customRedirectUri) {
        OAuthProperties.ProviderProperties kakaoProps = oAuthProperties != null ? oAuthProperties.kakao() : null;
        String clientId = kakaoProps != null ? kakaoProps.clientId() : null;
        if (clientId == null || clientId.isBlank()) {
            log.error("Kakao OAuth configuration is missing (client_id is not set)");
            throw new GeneralException(AuthErrorStatus.OAUTH_SERVER_ERROR);
        }

        String redirectUri = resolveRedirectUri(SocialType.KAKAO, customRedirectUri);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", clientId);
        body.add("redirect_uri", redirectUri);
        body.add("code", code);

        if (kakaoProps.clientSecret() != null && !kakaoProps.clientSecret().isBlank()) {
            body.add("client_secret", kakaoProps.clientSecret());
        }

        try {
            OAuthTokenResponse response = restClient.post()
                    .uri("https://kauth.kakao.com/oauth/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .body(OAuthTokenResponse.class);

            if (response == null || response.accessToken() == null || response.accessToken().isBlank()) {
                throw new GeneralException(AuthErrorStatus.OAUTH_CLIENT_ERROR);
            }
            return response.accessToken();
        } catch (GeneralException ge) {
            throw ge;
        } catch (Exception e) {
            log.warn("Kakao Authorization Code exchange failed", e);
            throw exceptionMapper.map(e, "Kakao Token Exchange");
        }
    }

    private OAuthUserInfo fetchKakaoUserInfo(String accessToken) {
        try {
            KakaoUserResponse response = restClient.get()
                    .uri("https://kapi.kakao.com/v2/user/me")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(KakaoUserResponse.class);

            if (response == null || response.id() == null) {
                throw new GeneralException(AuthErrorStatus.INVALID_AUTH_REQUEST);
            }

            String socialId = String.valueOf(response.id());
            if (socialId.isBlank() || "null".equalsIgnoreCase(socialId.trim())) {
                throw new GeneralException(AuthErrorStatus.INVALID_AUTH_REQUEST);
            }

            String profileImgUrl = null;
            if (response.kakaoAccount() != null && response.kakaoAccount().profile() != null) {
                profileImgUrl = response.kakaoAccount().profile().profileImageUrl();
            }

            return OAuthUserInfo.builder()
                    .socialId(socialId)
                    .profileImgUrl(profileImgUrl)
                    .build();

        } catch (Exception e) {
            throw exceptionMapper.map(e, "Kakao");
        }
    }

    private String exchangeGoogleCode(String code, String customRedirectUri) {
        OAuthProperties.ProviderProperties googleProps = oAuthProperties != null ? oAuthProperties.google() : null;
        String clientId = googleProps != null ? googleProps.clientId() : null;
        if (clientId == null || clientId.isBlank()) {
            log.error("Google OAuth configuration is missing (client_id is not set)");
            throw new GeneralException(AuthErrorStatus.OAUTH_SERVER_ERROR);
        }

        String redirectUri = resolveRedirectUri(SocialType.GOOGLE, customRedirectUri);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", clientId);
        body.add("client_secret", googleProps != null ? googleProps.clientSecret() : "");
        body.add("redirect_uri", redirectUri);
        body.add("code", code);

        try {
            OAuthTokenResponse response = restClient.post()
                    .uri("https://oauth2.googleapis.com/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .body(OAuthTokenResponse.class);

            if (response == null || response.accessToken() == null || response.accessToken().isBlank()) {
                throw new GeneralException(AuthErrorStatus.OAUTH_CLIENT_ERROR);
            }
            return response.accessToken();
        } catch (GeneralException ge) {
            throw ge;
        } catch (Exception e) {
            log.warn("Google Authorization Code exchange failed", e);
            throw exceptionMapper.map(e, "Google Token Exchange");
        }
    }

    private OAuthUserInfo fetchGoogleUserInfo(String accessToken) {
        try {
            GoogleUserResponse response = restClient.get()
                    .uri("https://www.googleapis.com/oauth2/v2/userinfo")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(GoogleUserResponse.class);

            if (response != null && response.id() != null && !response.id().isBlank() && !"null".equalsIgnoreCase(response.id().trim())) {
                return OAuthUserInfo.builder()
                        .socialId(response.id())
                        .profileImgUrl(response.picture())
                        .build();
            }
        } catch (Exception primaryEx) {
            if (isJwtFormat(accessToken)) {
                log.debug("Google v2 userinfo fetch failed for JWT-like token, attempting tokeninfo fallback: {}", primaryEx.getMessage());
                try {
                    String tokeninfoUrl = org.springframework.web.util.UriComponentsBuilder
                            .fromUriString("https://oauth2.googleapis.com/tokeninfo")
                            .queryParam("id_token", accessToken)
                            .build()
                            .toUriString();

                    GoogleUserResponse fallbackResponse = restClient.get()
                            .uri(tokeninfoUrl)
                            .accept(MediaType.APPLICATION_JSON)
                            .retrieve()
                            .body(GoogleUserResponse.class);

                    if (fallbackResponse != null && fallbackResponse.id() != null && !fallbackResponse.id().isBlank() && !"null".equalsIgnoreCase(fallbackResponse.id().trim())) {
                        return OAuthUserInfo.builder()
                                .socialId(fallbackResponse.id())
                                .profileImgUrl(fallbackResponse.picture())
                                .build();
                    }
                } catch (Exception fallbackEx) {
                    log.warn("Google tokeninfo fallback also failed: {}", fallbackEx.getMessage());
                }
            }
            throw exceptionMapper.map(primaryEx, "Google");
        }

        throw new GeneralException(AuthErrorStatus.INVALID_AUTH_REQUEST);
    }

    private boolean isJwtFormat(String token) {
        if (token == null) return false;
        int firstDot = token.indexOf('.');
        return firstDot > 0 && token.indexOf('.', firstDot + 1) > firstDot;
    }

    public boolean isBackendAllowedRedirectUri(SocialType socialType, String redirectUri) {
        if (redirectUri == null || redirectUri.isBlank()) {
            return false;
        }
        OAuthProperties.ProviderProperties providerProps = oAuthProperties != null ? switch (socialType) {
            case KAKAO -> oAuthProperties.kakao();
            case GOOGLE -> oAuthProperties.google();
        } : null;

        List<String> allowedUris = providerProps != null ? providerProps.getAllowedRedirectUris() : List.of();
        return allowedUris.contains(redirectUri.trim());
    }

    public boolean isFrontendAllowedRedirectUri(String redirectUri) {
        if (redirectUri == null || redirectUri.isBlank()) {
            return false;
        }
        List<String> allowedUris = oAuthProperties != null ? oAuthProperties.getAllowedFrontendRedirectUris() : List.of();
        return allowedUris.contains(redirectUri.trim());
    }

    private String resolveRedirectUri(SocialType socialType, String customRedirectUri) {
        OAuthProperties.ProviderProperties providerProps = oAuthProperties != null ? switch (socialType) {
            case KAKAO -> oAuthProperties.kakao();
            case GOOGLE -> oAuthProperties.google();
        } : null;

        List<String> allowedUris = providerProps != null ? providerProps.getAllowedRedirectUris() : List.of();

        if (allowedUris.isEmpty()) {
            log.error("{} OAuth configuration is missing (redirect_uri is not set)", socialType);
            throw new GeneralException(AuthErrorStatus.OAUTH_SERVER_ERROR);
        }

        if (customRedirectUri != null && !customRedirectUri.isBlank()) {
            String trimmedCustomUri = customRedirectUri.trim();
            if (allowedUris.contains(trimmedCustomUri)) {
                return trimmedCustomUri;
            }
            log.warn("{} custom redirect_uri [{}] is not in allowed list. Falling back to default [{}]", socialType, trimmedCustomUri, allowedUris.get(0));
        }

        return allowedUris.get(0);
    }

    public String getAuthorizationUrl(SocialType socialType, String customRedirectUri) {
        return getAuthorizationUrl(socialType, customRedirectUri, null);
    }

    public String getAuthorizationUrl(SocialType socialType, String customRedirectUri, String state) {
        OAuthProperties.ProviderProperties props = oAuthProperties != null ? switch (socialType) {
            case KAKAO -> oAuthProperties.kakao();
            case GOOGLE -> oAuthProperties.google();
        } : null;

        String clientId = props != null ? props.clientId() : null;
        if (clientId == null || clientId.isBlank()) {
            log.error("{} OAuth configuration is missing (client_id is not set)", socialType);
            throw new GeneralException(AuthErrorStatus.OAUTH_SERVER_ERROR);
        }

        String redirectUri = resolveRedirectUri(socialType, customRedirectUri);

        org.springframework.web.util.UriComponentsBuilder builder = switch (socialType) {
            case KAKAO -> org.springframework.web.util.UriComponentsBuilder.fromUriString("https://kauth.kakao.com/oauth/authorize")
                    .queryParam("response_type", "code")
                    .queryParam("client_id", clientId)
                    .queryParam("redirect_uri", redirectUri);
            case GOOGLE -> org.springframework.web.util.UriComponentsBuilder.fromUriString("https://accounts.google.com/o/oauth2/v2/auth")
                    .queryParam("response_type", "code")
                    .queryParam("client_id", clientId)
                    .queryParam("redirect_uri", redirectUri)
                    .queryParam("scope", "email profile openid");
        };

        if (state != null && !state.isBlank()) {
            builder.queryParam("state", state);
        }

        return builder.build().toUriString();
    }

    public String buildFrontendRedirectUrl(String code) {
        return buildFrontendRedirectUrl(code, (String) null);
    }

    public String buildFrontendRedirectUrl(String code, String customFrontendRedirectUri) {
        String baseUrl = (customFrontendRedirectUri != null && isFrontendAllowedRedirectUri(customFrontendRedirectUri))
                ? customFrontendRedirectUri.trim()
                : (oAuthProperties != null && oAuthProperties.frontendRedirectUri() != null && !oAuthProperties.frontendRedirectUri().isBlank())
                ? oAuthProperties.frontendRedirectUri()
                : "http://localhost:3000/oauth/callback";

        return org.springframework.web.util.UriComponentsBuilder.fromUriString(baseUrl)
                .queryParam("code", code)
                .build()
                .toUriString();
    }

    public String buildFrontendRedirectUrl(com.mr.domain.auth.dto.res.AuthResponseDTO.LoginResponse loginResponse) {
        return buildFrontendRedirectUrl(loginResponse, null);
    }

    public String buildFrontendRedirectUrl(com.mr.domain.auth.dto.res.AuthResponseDTO.LoginResponse loginResponse, String customFrontendRedirectUri) {
        String baseUrl = (customFrontendRedirectUri != null && isFrontendAllowedRedirectUri(customFrontendRedirectUri))
                ? customFrontendRedirectUri.trim()
                : (oAuthProperties != null && oAuthProperties.frontendRedirectUri() != null && !oAuthProperties.frontendRedirectUri().isBlank())
                ? oAuthProperties.frontendRedirectUri()
                : "http://localhost:3000/oauth/callback";

        return org.springframework.web.util.UriComponentsBuilder.fromUriString(baseUrl)
                .queryParam("accessToken", loginResponse.tokenInfo().accessToken())
                .queryParam("refreshToken", loginResponse.tokenInfo().refreshToken())
                .queryParam("userId", loginResponse.userId())
                .queryParam("isNewUser", loginResponse.isNewUser())
                .queryParam("isOnboardingCompleted", loginResponse.isOnboardingCompleted())
                .build()
                .toUriString();
    }

    public String buildFrontendErrorRedirectUrl(String error) {
        return buildFrontendErrorRedirectUrl(error, (String) null);
    }

    public String buildFrontendErrorRedirectUrl(String error, String customFrontendRedirectUri) {
        String baseUrl = (customFrontendRedirectUri != null && isFrontendAllowedRedirectUri(customFrontendRedirectUri))
                ? customFrontendRedirectUri.trim()
                : (oAuthProperties != null && oAuthProperties.frontendRedirectUri() != null && !oAuthProperties.frontendRedirectUri().isBlank())
                ? oAuthProperties.frontendRedirectUri()
                : "http://localhost:3000/oauth/callback";

        return org.springframework.web.util.UriComponentsBuilder.fromUriString(baseUrl)
                .queryParam("error", (error != null && !error.isBlank()) ? error : "authentication_failed")
                .build()
                .toUriString();
    }
}
