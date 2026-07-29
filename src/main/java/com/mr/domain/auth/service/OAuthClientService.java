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

    public OAuthUserInfo getUserInfo(SocialType socialType, String codeOrToken, String customRedirectUri) {
        return switch (socialType) {
            case kakao -> getKakaoUserInfo(codeOrToken, customRedirectUri);
            case google -> getGoogleUserInfo(codeOrToken, customRedirectUri);
        };
    }

    private String exchangeKakaoCode(String code, String customRedirectUri) {
        OAuthProperties.ProviderProperties kakaoProps = oAuthProperties != null ? oAuthProperties.kakao() : null;
        String clientId = kakaoProps != null ? kakaoProps.clientId() : null;
        if (clientId == null || clientId.isBlank()) {
            return code;
        }

        String redirectUri = (customRedirectUri != null && !customRedirectUri.isBlank())
                ? customRedirectUri
                : (kakaoProps != null ? kakaoProps.redirectUri() : "");

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
        } catch (Exception e) {
            log.warn("Kakao Authorization Code exchange failed or input was an AccessToken. Trying as AccessToken.");
            return code;
        }
    }

    private OAuthUserInfo getKakaoUserInfo(String codeOrToken, String customRedirectUri) {
        try {
            String accessToken = exchangeKakaoCode(codeOrToken, customRedirectUri);

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
            return code;
        }

        String redirectUri = (customRedirectUri != null && !customRedirectUri.isBlank())
                ? customRedirectUri
                : (googleProps != null ? googleProps.redirectUri() : "");

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
        } catch (Exception e) {
            log.warn("Google Authorization Code exchange failed or input was an AccessToken. Trying as AccessToken.");
            return code;
        }
    }

    private OAuthUserInfo getGoogleUserInfo(String codeOrToken, String customRedirectUri) {
        try {
            String accessToken = exchangeGoogleCode(codeOrToken, customRedirectUri);

            GoogleUserResponse response = restClient.get()
                    .uri("https://www.googleapis.com/oauth2/v2/userinfo")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(GoogleUserResponse.class);

            if (response == null || response.id() == null || response.id().isBlank() || "null".equalsIgnoreCase(response.id().trim())) {
                throw new GeneralException(AuthErrorStatus.INVALID_AUTH_REQUEST);
            }

            return OAuthUserInfo.builder()
                    .socialId(response.id())
                    .profileImgUrl(response.picture())
                    .build();
        } catch (Exception e) {
            throw exceptionMapper.map(e, "Google");
        }
    }
}
