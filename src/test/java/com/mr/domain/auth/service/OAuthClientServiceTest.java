package com.mr.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mr.domain.auth.entity.enums.SocialType;
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
    @DisplayName("카카오 response의 id가 null이면 GeneralException이 발생한다")
    void getKakaoUserInfo_nullId_throwsException() {
        mockServer.expect(MockRestRequestMatchers.requestTo("https://kapi.kakao.com/v2/user/me"))
                .andRespond(MockRestResponseCreators.withSuccess("{\"id\": null}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> oAuthClientService.getUserInfo(SocialType.KAKAO, "token"))
                .isInstanceOf(GeneralException.class);
    }

    @Test
    @DisplayName("카카오 response의 id가 'null' 문자열이면 GeneralException이 발생한다")
    void getKakaoUserInfo_literalNullId_throwsException() {
        mockServer.expect(MockRestRequestMatchers.requestTo("https://kapi.kakao.com/v2/user/me"))
                .andRespond(MockRestResponseCreators.withSuccess("{\"id\": \"null\"}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> oAuthClientService.getUserInfo(SocialType.KAKAO, "token"))
                .isInstanceOf(GeneralException.class);
    }

    @Test
    @DisplayName("구글 response의 id가 없으면 GeneralException이 발생한다")
    void getGoogleUserInfo_missingId_throwsException() {
        mockServer.expect(MockRestRequestMatchers.requestTo("https://www.googleapis.com/oauth2/v2/userinfo"))
                .andRespond(MockRestResponseCreators.withSuccess("{\"email\": \"test@example.com\"}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> oAuthClientService.getUserInfo(SocialType.GOOGLE, "token"))
                .isInstanceOf(GeneralException.class);
    }
}
