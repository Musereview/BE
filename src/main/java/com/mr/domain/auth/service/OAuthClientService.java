package com.mr.domain.auth.service;

import com.mr.domain.auth.dto.OAuthUserInfo;
import com.mr.domain.auth.dto.res.GoogleUserResponse;
import com.mr.domain.auth.dto.res.KakaoUserResponse;
import com.mr.domain.auth.entity.enums.SocialType;
import com.mr.domain.auth.exception.AuthErrorStatus;
import com.mr.domain.auth.exception.OAuthExceptionMapper;
import com.mr.global.apipayload.exception.GeneralException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
public class OAuthClientService {

    private final RestClient restClient;
    private final OAuthExceptionMapper exceptionMapper;

    public OAuthClientService(
            @Qualifier("oauthRestClient") RestClient restClient,
            OAuthExceptionMapper exceptionMapper) {
        this.restClient = restClient;
        this.exceptionMapper = exceptionMapper;
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
            throw exceptionMapper.map(e, "Kakao");
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
            throw exceptionMapper.map(e, "Google");
        }
    }
}