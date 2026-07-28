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
        oAuthClientService = new OAuthClientService(restClientBuilder.build(), new OAuthExceptionMapper());
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
}
