package com.mr.domain.auth.service;

import com.mr.domain.auth.dto.OAuthUserInfo;
import com.mr.domain.auth.entity.enums.SocialType;
import com.mr.global.apipayload.code.CommonStatus;
import com.mr.global.apipayload.exception.GeneralException;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

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
            Map response = restClient.get()
                    .uri("https://kapi.kakao.com/v2/user/me")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(Map.class);

            String socialId = String.valueOf(response.get("id"));
            Map kakaoAccount = (Map) response.get("kakao_account");

            Map profile = kakaoAccount != null ? (Map) kakaoAccount.get("profile") : null;
            String profileImgUrl = profile != null ? (String) profile.get("profile_image_url") : null;

            return OAuthUserInfo.builder()
                    .socialId(socialId)
                    .profileImgUrl(profileImgUrl)
                    .build();
        } catch (Exception e) {
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