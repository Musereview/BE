package com.mr.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mr.domain.auth.entity.enums.SocialType;
import com.mr.domain.auth.exception.AuthErrorStatus;
import com.mr.global.apipayload.exception.GeneralException;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
        oAuthClientService = new OAuthClientService(restClientBuilder.build());
    }

    @Test
    @DisplayName("OAuthClientService 생성 시 connect/read timeout 설정이 정상 적용된다")
    void createOAuthClientService_withTimeout() {
        Duration connectTimeout = Duration.ofSeconds(3);
        Duration readTimeout = Duration.ofSeconds(5);

        OAuthClientService service = new OAuthClientService(connectTimeout, readTimeout);

        assertThat(service).isNotNull();
    }

    @Test
    @DisplayName("카카오 response의 id가 null이면 GeneralException(INVALID_AUTH_REQUEST)이 발생한다")
    void getKakaoUserInfo_nullId_throwsException() {
        mockServer.expect(MockRestRequestMatchers.requestTo("https://kapi.kakao.com/v2/user/me"))
                .andRespond(MockRestResponseCreators.withSuccess("{\"id\": null}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> oAuthClientService.getUserInfo(SocialType.KAKAO, "token"))
                .isInstanceOf(GeneralException.class)
                .satisfies(e -> assertThat(((GeneralException) e).getCode()).isEqualTo(AuthErrorStatus.INVALID_AUTH_REQUEST));
    }

    @Test
    @DisplayName("카카오 response의 id가 'null' 문자열이면 GeneralException이 발생한다")
    void getKakaoUserInfo_literalNullId_throwsException() {
        mockServer.expect(MockRestRequestMatchers.requestTo("https://kapi.kakao.com/v2/user/me"))
                .andRespond(MockRestResponseCreators.withSuccess("{\"id\": \"null\"}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> oAuthClientService.getUserInfo(SocialType.KAKAO, "token"))
                .isInstanceOf(GeneralException.class)
                .satisfies(e -> assertThat(((GeneralException) e).getCode()).isEqualTo(AuthErrorStatus.INVALID_AUTH_REQUEST));
    }

    @Test
    @DisplayName("구글 response의 id가 없으면 GeneralException이 발생한다")
    void getGoogleUserInfo_missingId_throwsException() {
        mockServer.expect(MockRestRequestMatchers.requestTo("https://www.googleapis.com/oauth2/v2/userinfo"))
                .andRespond(MockRestResponseCreators.withSuccess("{\"email\": \"test@example.com\"}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> oAuthClientService.getUserInfo(SocialType.GOOGLE, "token"))
                .isInstanceOf(GeneralException.class)
                .satisfies(e -> assertThat(((GeneralException) e).getCode()).isEqualTo(AuthErrorStatus.INVALID_AUTH_REQUEST));
    }

    @Test
    @DisplayName("카카오 OAuth 서버가 401 Unauthorized를 반환하면 OAUTH_CLIENT_ERROR 예외로 매핑된다")
    void getKakaoUserInfo_401Error_throwsOauthClientError() {
        mockServer.expect(MockRestRequestMatchers.requestTo("https://kapi.kakao.com/v2/user/me"))
                .andRespond(MockRestResponseCreators.withUnauthorizedRequest());

        assertThatThrownBy(() -> oAuthClientService.getUserInfo(SocialType.KAKAO, "invalid_token"))
                .isInstanceOf(GeneralException.class)
                .satisfies(e -> assertThat(((GeneralException) e).getCode()).isEqualTo(AuthErrorStatus.OAUTH_CLIENT_ERROR));
    }

    @Test
    @DisplayName("구글 OAuth 서버가 500 Internal Server Error를 반환하면 OAUTH_SERVER_ERROR 예외로 매핑된다")
    void getGoogleUserInfo_500Error_throwsOauthServerError() {
        mockServer.expect(MockRestRequestMatchers.requestTo("https://www.googleapis.com/oauth2/v2/userinfo"))
                .andRespond(MockRestResponseCreators.withServerError());

        assertThatThrownBy(() -> oAuthClientService.getUserInfo(SocialType.GOOGLE, "token"))
                .isInstanceOf(GeneralException.class)
                .satisfies(e -> assertThat(((GeneralException) e).getCode()).isEqualTo(AuthErrorStatus.OAUTH_SERVER_ERROR));
    }
}
