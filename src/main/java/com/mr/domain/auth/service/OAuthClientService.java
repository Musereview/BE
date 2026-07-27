package com.mr.domain.auth.service;

import com.mr.domain.auth.dto.OAuthUserInfo;
import com.mr.domain.auth.entity.enums.SocialType;
import com.mr.global.apipayload.code.CommonStatus;
import com.mr.global.apipayload.exception.GeneralException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
@Slf4j
@Service
public class OAuthClientService {

    private final RestClient restClient = RestClient.create();

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
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});

            if (response == null) {
                throw new GeneralException(CommonStatus.INVALID_INPUT_VALUE);
            }

            String socialId = String.valueOf(response.get("id"));

            @SuppressWarnings("unchecked")
            Map<String, Object> kakaoAccount = (Map<String, Object>) response.get("kakao_account");

            @SuppressWarnings("unchecked")
            Map<String, Object> profile = kakaoAccount != null ? (Map<String, Object>) kakaoAccount.get("profile") : null;
            String profileImgUrl = profile != null ? (String) profile.get("profile_image_url") : null;

            return OAuthUserInfo.builder()
                    .socialId(socialId)
                    .profileImgUrl(profileImgUrl)
                    .build();

        } catch (HttpClientErrorException e) {
            // 🌟 카카오 서버가 거부한 진짜 이유(HTTP 상태코드 및 응답 바디)를 콘솔에 출력합니다!
            log.error("카카오 OAuth API 호출 실패 - Status: {}, Response: {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new GeneralException(CommonStatus.INVALID_INPUT_VALUE);
        } catch (Exception e) {
            log.error("OAuth 처리 중 자바 내부 예외 발생", e);
            throw new GeneralException(CommonStatus.INVALID_INPUT_VALUE);
        }
    }

    private OAuthUserInfo getGoogleUserInfo(String accessToken) {
        try {
            Map response = restClient.get()
                    .uri("https://www.googleapis.com/oauth2/v2/userinfo")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(Map.class);

            String socialId = (String) response.get("id");
            String email = (String) response.get("email");
            String profileImgUrl = (String) response.get("picture");

            return OAuthUserInfo.builder()
                    .socialId(socialId)
                    .profileImgUrl(profileImgUrl)
                    .build();
        } catch (Exception e) {
            throw new GeneralException(CommonStatus.INVALID_INPUT_VALUE);
        }
    }
}