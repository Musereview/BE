package com.mr.domain.auth.service;

import com.mr.domain.auth.dto.OAuthUserInfo;
import com.mr.domain.auth.entity.enums.SocialType;
import com.mr.global.apipayload.code.CommonStatus;
import com.mr.global.apipayload.exception.GeneralException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Map;

@Slf4j
@Service
public class OAuthClientService {

    private final RestClient restClient;

    public OAuthClientService(
            @Value("${oauth.connect-timeout:3s}") Duration connectTimeout,
            @Value("${oauth.read-timeout:5s}") Duration readTimeout) {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(connectTimeout)
                .withReadTimeout(readTimeout);

        this.restClient = RestClient.builder()
                .requestFactory(ClientHttpRequestFactories.get(settings))
                .build();
    }

    public OAuthClientService(RestClient restClient) {
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
            Map<String, Object> response = restClient.get()
                    .uri("https://kapi.kakao.com/v2/user/me")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .header(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded;charset=utf-8")
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {
                    });

            String socialId = extractSocialId(response);

            @SuppressWarnings("unchecked")
            Map<String, Object> kakaoAccount = (Map<String, Object>) response.get("kakao_account");

            @SuppressWarnings("unchecked")
            Map<String, Object> profile = kakaoAccount != null ? (Map<String, Object>) kakaoAccount.get("profile") : null;
            String profileImgUrl = profile != null ? (String) profile.get("profile_image_url") : null;

            return OAuthUserInfo.builder()
                    .socialId(socialId)
                    .profileImgUrl(profileImgUrl)
                    .build();

        } catch (GeneralException e) {
            throw e;
        } catch (Exception e) {
            throw new GeneralException(CommonStatus.INVALID_INPUT_VALUE);
        }
    }

    private OAuthUserInfo getGoogleUserInfo(String accessToken) {
        try {
            Map<String, Object> response = restClient.get()
                    .uri("https://www.googleapis.com/oauth2/v2/userinfo")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {
                    });

            String socialId = extractSocialId(response);
            String profileImgUrl = response != null ? (String) response.get("picture") : null;

            return OAuthUserInfo.builder()
                    .socialId(socialId)
                    .profileImgUrl(profileImgUrl)
                    .build();
        } catch (GeneralException e) {
            throw e;
        } catch (Exception e) {
            throw new GeneralException(CommonStatus.INVALID_INPUT_VALUE);
        }
    }

    private String extractSocialId(Map<String, Object> response) {
        if (response == null) {
            throw new GeneralException(CommonStatus.INVALID_INPUT_VALUE);
        }
        Object idObj = response.get("id");
        if (idObj == null) {
            throw new GeneralException(CommonStatus.INVALID_INPUT_VALUE);
        }
        String socialId = String.valueOf(idObj);
        if (socialId.isBlank() || "null".equalsIgnoreCase(socialId.trim())) {
            throw new GeneralException(CommonStatus.INVALID_INPUT_VALUE);
        }
        return socialId;
    }
}