package com.mr.domain.auth.service;

import com.mr.domain.auth.dto.OAuthUserInfo;
import com.mr.domain.auth.dto.res.GoogleUserResponse;
import com.mr.domain.auth.dto.res.KakaoUserResponse;
import com.mr.domain.auth.entity.enums.SocialType;
import com.mr.domain.auth.exception.AuthErrorStatus;
import com.mr.global.apipayload.exception.GeneralException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Service
public class OAuthClientService {

    private final RestClient restClient;

    public OAuthClientService(@Qualifier("oauthRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public OAuthUserInfo getUserInfo(SocialType socialType, String accessToken) {
        return switch (socialType) {
            case KAKAO -> getKakaoUserInfo(accessToken);
            case GOOGLE -> getGoogleUserInfo(accessToken);
        };
    }

    private OAuthUserInfo getKakaoUserInfo(String accessToken) {
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
            throw mapOAuthException(e, "Kakao");
        }
    }

    private OAuthUserInfo getGoogleUserInfo(String accessToken) {
        try {
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
            throw mapOAuthException(e, "Google");
        }
    }

    private GeneralException mapOAuthException(Exception e, String provider) {
        if (e instanceof GeneralException ge) {
            return ge;
        }
        if (e instanceof HttpClientErrorException clientErr) {
            log.warn("{} OAuth 클라이언트 인증 오류 (status={}): {}", provider, clientErr.getStatusCode(), clientErr.getMessage());
            return new GeneralException(AuthErrorStatus.OAUTH_CLIENT_ERROR);
        }
        if (e instanceof HttpServerErrorException serverErr) {
            log.error("{} OAuth 서버 오류 (status={}): {}", provider, serverErr.getStatusCode(), serverErr.getMessage());
            return new GeneralException(AuthErrorStatus.OAUTH_SERVER_ERROR);
        }
        if (e instanceof ResourceAccessException netErr) {
            log.error("{} OAuth 타임아웃/네트워크 통신 오류: {}", provider, netErr.getMessage());
            return new GeneralException(AuthErrorStatus.OAUTH_SERVER_ERROR);
        }
        if (e instanceof RestClientResponseException rcre) {
            log.error("{} OAuth HTTP 응답 예외 (status={}): {}", provider, rcre.getStatusCode(), rcre.getMessage());
            if (rcre.getStatusCode().is4xxClientError()) {
                return new GeneralException(AuthErrorStatus.OAUTH_CLIENT_ERROR);
            } else {
                return new GeneralException(AuthErrorStatus.OAUTH_SERVER_ERROR);
            }
        }
        log.error("{} OAuth 사용자 정보 처리 중 예외 발생", provider, e);
        return new GeneralException(AuthErrorStatus.INVALID_AUTH_REQUEST);
    }
}