package com.mr.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mr.domain.auth.dto.OAuthUserInfo;
import com.mr.domain.auth.entity.enums.SocialType;
import com.mr.domain.auth.exception.AuthErrorStatus;
import com.mr.domain.auth.exception.OAuthExceptionMapper;
import com.mr.global.apipayload.exception.GeneralException;
import com.mr.global.config.OAuthRestClientConfig;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestClient;

class OAuthClientServiceTest {

    private RestClient.Builder restClientBuilder;
    private MockRestServiceServer mockServer;
    private OAuthClientService oAuthClientService;

    @BeforeEach
    void setUp() {
        restClientBuilder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
        oAuthClientService = new OAuthClientService(restClientBuilder.build(), new OAuthExceptionMapper(), null);
    }

    @AfterEach
    void tearDown() {
        mockServer.verify();
    }

    @Test
    @DisplayName("OAuthRestClientConfig 생성 시 connect/read timeout 설정이 정상 적용된 RestClient가 생성된다")
    void createOAuthRestClient_withTimeout() {
        Duration connectTimeout = Duration.ofSeconds(3);
        Duration readTimeout = Duration.ofSeconds(5);

        OAuthRestClientConfig config = new OAuthRestClientConfig();
        RestClient client = config.oauthRestClient(connectTimeout, readTimeout);

        assertThat(client).isNotNull();
    }

    @Test
    @DisplayName("카카오 response의 id가 null이면 GeneralException(INVALID_AUTH_REQUEST)이 발생한다")
    void getKakaoUserInfo_nullId_throwsException() {
        mockServer.expect(MockRestRequestMatchers.requestTo("https://kapi.kakao.com/v2/user/me"))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, "Bearer token"))
                .andRespond(MockRestResponseCreators.withSuccess("{\"id\": null}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> oAuthClientService.getUserInfo(SocialType.KAKAO, "token"))
                .isInstanceOf(GeneralException.class)
                .satisfies(e -> assertThat(((GeneralException) e).getCode()).isEqualTo(AuthErrorStatus.INVALID_AUTH_REQUEST));
    }

    @Test
    @DisplayName("카카오 response의 id가 'null' 문자열이면 GeneralException이 발생한다")
    void getKakaoUserInfo_literalNullId_throwsException() {
        mockServer.expect(MockRestRequestMatchers.requestTo("https://kapi.kakao.com/v2/user/me"))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, "Bearer token"))
                .andRespond(MockRestResponseCreators.withSuccess("{\"id\": \"null\"}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> oAuthClientService.getUserInfo(SocialType.KAKAO, "token"))
                .isInstanceOf(GeneralException.class)
                .satisfies(e -> assertThat(((GeneralException) e).getCode()).isEqualTo(AuthErrorStatus.INVALID_AUTH_REQUEST));
    }

    @Test
    @DisplayName("구글 response의 id가 없으면 GeneralException이 발생한다")
    void getGoogleUserInfo_missingId_throwsException() {
        mockServer.expect(MockRestRequestMatchers.requestTo("https://www.googleapis.com/oauth2/v2/userinfo"))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, "Bearer token"))
                .andRespond(MockRestResponseCreators.withSuccess("{\"email\": \"test@example.com\"}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> oAuthClientService.getUserInfo(SocialType.GOOGLE, "token"))
                .isInstanceOf(GeneralException.class)
                .satisfies(e -> assertThat(((GeneralException) e).getCode()).isEqualTo(AuthErrorStatus.INVALID_AUTH_REQUEST));
    }

    @Test
    @DisplayName("카카오 OAuth 서버가 401 Unauthorized를 반환하면 OAUTH_CLIENT_ERROR 예외로 매핑된다")
    void getKakaoUserInfo_401Error_throwsOauthClientError() {
        mockServer.expect(MockRestRequestMatchers.requestTo("https://kapi.kakao.com/v2/user/me"))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, "Bearer invalid_token"))
                .andRespond(MockRestResponseCreators.withUnauthorizedRequest());

        assertThatThrownBy(() -> oAuthClientService.getUserInfo(SocialType.KAKAO, "invalid_token"))
                .isInstanceOf(GeneralException.class)
                .satisfies(e -> assertThat(((GeneralException) e).getCode()).isEqualTo(AuthErrorStatus.OAUTH_CLIENT_ERROR));
    }

    @Test
    @DisplayName("구글 OAuth 서버가 500 Internal Server Error를 반환하면 OAUTH_SERVER_ERROR 예외로 매핑된다")
    void getGoogleUserInfo_500Error_throwsOauthServerError() {
        mockServer.expect(MockRestRequestMatchers.requestTo("https://www.googleapis.com/oauth2/v2/userinfo"))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, "Bearer token"))
                .andRespond(MockRestResponseCreators.withServerError());

        assertThatThrownBy(() -> oAuthClientService.getUserInfo(SocialType.GOOGLE, "token"))
                .isInstanceOf(GeneralException.class)
                .satisfies(e -> assertThat(((GeneralException) e).getCode()).isEqualTo(AuthErrorStatus.OAUTH_SERVER_ERROR));
    }

    @Test
    @DisplayName("카카오 정상 응답 시 OAuthUserInfo(socialId, profileImgUrl)로 성공적으로 매핑된다")
    void getKakaoUserInfo_success() {
        String jsonResponse = """
                {
                    "id": 123456789,
                    "kakao_account": {
                        "profile": {
                            "profile_image_url": "https://example.com/kakao_profile.png"
                        }
                    }
                }
                """;

        mockServer.expect(MockRestRequestMatchers.requestTo("https://kapi.kakao.com/v2/user/me"))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, "Bearer valid_token"))
                .andRespond(MockRestResponseCreators.withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        OAuthUserInfo userInfo = oAuthClientService.getUserInfo(SocialType.KAKAO, "valid_token");

        assertThat(userInfo.socialId()).isEqualTo("123456789");
        assertThat(userInfo.profileImgUrl()).isEqualTo("https://example.com/kakao_profile.png");
    }

    @Test
    @DisplayName("구글 정상 응답 시 OAuthUserInfo(socialId, profileImgUrl)로 성공적으로 매핑된다")
    void getGoogleUserInfo_success() {
        String jsonResponse = """
                {
                    "id": "google_987654321",
                    "picture": "https://example.com/google_profile.png",
                    "email": "user@gmail.com"
                }
                """;

        mockServer.expect(MockRestRequestMatchers.requestTo("https://www.googleapis.com/oauth2/v2/userinfo"))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, "Bearer valid_token"))
                .andRespond(MockRestResponseCreators.withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        OAuthUserInfo userInfo = oAuthClientService.getUserInfo(SocialType.GOOGLE, "valid_token");

        assertThat(userInfo.socialId()).isEqualTo("google_987654321");
        assertThat(userInfo.profileImgUrl()).isEqualTo("https://example.com/google_profile.png");
    }

    @Test
    @DisplayName("구글 ID Token fallback은 설정된 clientId와 audience가 일치해야 성공")
    void getGoogleUserInfo_idToken_validatesAudience() {
        String idToken = "header.payload.signature";
        com.mr.global.config.OAuthProperties.ProviderProperties googleProps =
                new com.mr.global.config.OAuthProperties.ProviderProperties(
                        "google-client-id",
                        "google-client-secret",
                        "http://localhost:8080/api/auth/google/callback",
                        java.util.List.of("http://localhost:8080/api/auth/google/callback")
                );
        com.mr.global.config.OAuthProperties properties = new com.mr.global.config.OAuthProperties(
                "http://localhost:5173/oauth/callback",
                java.util.List.of("http://localhost:5173/oauth/callback"),
                null,
                googleProps
        );
        OAuthClientService serviceWithProps = new OAuthClientService(
                restClientBuilder.build(), new OAuthExceptionMapper(), properties);

        mockServer.expect(MockRestRequestMatchers.requestTo("https://www.googleapis.com/oauth2/v2/userinfo"))
                .andRespond(MockRestResponseCreators.withUnauthorizedRequest());
        mockServer.expect(MockRestRequestMatchers.requestTo(
                        "https://oauth2.googleapis.com/tokeninfo?id_token=header.payload.signature"))
                .andRespond(MockRestResponseCreators.withSuccess(
                        """
                        {"sub":"google-user","aud":"google-client-id","picture":"https://example.com/profile.png"}
                        """,
                        MediaType.APPLICATION_JSON
                ));

        OAuthUserInfo userInfo = serviceWithProps.getUserInfo(SocialType.GOOGLE, idToken);

        assertThat(userInfo.socialId()).isEqualTo("google-user");
    }

    @Test
    @DisplayName("OAuth 설정(clientId)이 누락된 경우 authorization_code 교환 요청 시 OAUTH_SERVER_ERROR 예외가 발생한다")
    void exchangeCode_missingConfig_throwsOauthServerError() {
        assertThatThrownBy(() -> oAuthClientService.getUserInfoByCode(SocialType.KAKAO, "sample_code", null))
                .isInstanceOf(GeneralException.class)
                .satisfies(e -> assertThat(((GeneralException) e).getCode()).isEqualTo(AuthErrorStatus.OAUTH_SERVER_ERROR));
    }

    @Test
    @DisplayName("허용되지 않은 customRedirectUri로 authorization_code 교환 요청 시 기본 redirectUri로 자동 폴백 처리된다")
    void exchangeCode_invalidCustomRedirectUri_fallsBackToDefaultUri() {
        com.mr.global.config.OAuthProperties.ProviderProperties kakaoProps = new com.mr.global.config.OAuthProperties.ProviderProperties(
                "sample_client_id",
                "sample_secret",
                "https://allowed.example.com/oauth/callback",
                java.util.List.of("https://allowed.example.com/oauth/callback")
        );
        com.mr.global.config.OAuthProperties oAuthProperties = new com.mr.global.config.OAuthProperties(
                "http://localhost:3000/oauth/callback", java.util.List.of("http://localhost:3000/oauth/callback"), kakaoProps, null);
        OAuthClientService serviceWithProps = new OAuthClientService(
                restClientBuilder.build(), new OAuthExceptionMapper(), oAuthProperties);

        mockServer.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo("https://kauth.kakao.com/oauth/token"))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withServerError());

        assertThatThrownBy(() -> serviceWithProps.getUserInfoByCode(SocialType.KAKAO, "sample_code", "https://unauthorized.malicious.com/callback"))
                .isInstanceOf(GeneralException.class);
    }

    @Test
    @DisplayName("Bearer 접두어가 포함된 accessToken이 입력되어도 정상적으로 제거 후 처리된다")
    void getUserInfo_bearerPrefix_stripsPrefixAndSucceeds() {
        String jsonResponse = """
                {
                    "id": 999888777,
                    "kakao_account": {
                        "profile": {
                            "profile_image_url": "https://example.com/profile.png"
                        }
                    }
                }
                """;

        mockServer.expect(MockRestRequestMatchers.requestTo("https://kapi.kakao.com/v2/user/me"))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, "Bearer valid_token"))
                .andRespond(MockRestResponseCreators.withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        OAuthUserInfo userInfo = oAuthClientService.getUserInfo(SocialType.KAKAO, "Bearer valid_token");

        assertThat(userInfo.socialId()).isEqualTo("999888777");
    }

    @Test
    @DisplayName("buildFrontendRedirectUrl 및 buildFrontendErrorRedirectUrl에 허용된 customFrontendRedirectUri가 지정된 경우 해당 URI를 base로 URL이 구성된다")
    void buildFrontendRedirectUrl_customUri_usesCustomUriBase() {
        String customUri = "https://myfrontend.com/custom/callback";
        com.mr.global.config.OAuthProperties oAuthProperties = new com.mr.global.config.OAuthProperties(
                "http://localhost:5173/oauth/callback", java.util.List.of("http://localhost:5173/oauth/callback", customUri), null, null);
        OAuthClientService serviceWithProps = new OAuthClientService(
                restClientBuilder.build(), new OAuthExceptionMapper(), oAuthProperties);

        String successUrl = serviceWithProps.buildFrontendRedirectUrl("tempCode123", customUri);
        String errorUrl = serviceWithProps.buildFrontendErrorRedirectUrl("access_denied", customUri);

        assertThat(successUrl).isEqualTo("https://myfrontend.com/custom/callback?code=tempCode123");
        assertThat(errorUrl).isEqualTo("https://myfrontend.com/custom/callback?error=access_denied");
    }

    @Test
    @DisplayName("buildFrontendRedirectUrl에 허용되지 않은 악성 customFrontendRedirectUri가 들어오면 기본 URI로 폴백되어 Open Redirect를 방지한다")
    void buildFrontendRedirectUrl_unallowedCustomUri_fallsBackToDefault() {
        String maliciousUri = "https://evil.example.com/collect";
        com.mr.global.config.OAuthProperties oAuthProperties = new com.mr.global.config.OAuthProperties(
                "http://localhost:5173/oauth/callback", java.util.List.of("http://localhost:5173/oauth/callback"), null, null);
        OAuthClientService serviceWithProps = new OAuthClientService(
                restClientBuilder.build(), new OAuthExceptionMapper(), oAuthProperties);

        String successUrl = serviceWithProps.buildFrontendRedirectUrl("tempCode123", maliciousUri);
        assertThat(successUrl).startsWith("http://localhost:5173/oauth/callback");
    }

    @Test
    @DisplayName("isBackendAllowedRedirectUri는 등록된 백엔드 콜백 URI 여부를 정확히 판단한다")
    void isBackendAllowedRedirectUri_validatesCorrectly() {
        com.mr.global.config.OAuthProperties.ProviderProperties kakaoProps = new com.mr.global.config.OAuthProperties.ProviderProperties(
                "sample_client_id",
                "sample_secret",
                "http://localhost:8080/api/auth/kakao/callback",
                java.util.List.of("http://localhost:8080/api/auth/kakao/callback")
        );
        com.mr.global.config.OAuthProperties oAuthProperties = new com.mr.global.config.OAuthProperties(
                "http://localhost:5173/oauth/callback", java.util.List.of("http://localhost:5173/oauth/callback"), kakaoProps, null);
        OAuthClientService serviceWithProps = new OAuthClientService(
                restClientBuilder.build(), new OAuthExceptionMapper(), oAuthProperties);

        boolean isBackendUri = serviceWithProps.isBackendAllowedRedirectUri(SocialType.KAKAO, "http://localhost:8080/api/auth/kakao/callback");
        boolean isFrontendUri = serviceWithProps.isBackendAllowedRedirectUri(SocialType.KAKAO, "http://localhost:5173/custom/callback");

        assertThat(isBackendUri).isTrue();
        assertThat(isFrontendUri).isFalse();
    }
}
