package com.mr.global.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mr.domain.auth.entity.enums.SocialType;
import com.mr.domain.auth.service.AuthService;
import com.mr.domain.auth.service.OAuthClientService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OAuthClientService oAuthClientService;

    @MockBean
    private AuthService authService;

    @Test
    @DisplayName("GET /api/auth/login/KAKAO 요청은 인증 없이 permitAll 처리되어 302 리다이렉트가 수행된다")
    void startOAuthLogin_permitAll_redirectsSuccessfully() throws Exception {
        given(oAuthClientService.getAuthorizationUrl(any(SocialType.class), any(), any()))
                .willReturn("https://kauth.kakao.com/oauth/authorize?client_id=sample");

        mockMvc.perform(get("/api/auth/login/KAKAO"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("GET /api/auth/kakao/callback 요청은 인증 없이 permitAll 처리되어 302 리다이렉트가 수행된다")
    void oAuthCallback_permitAll_redirectsSuccessfully() throws Exception {
        given(oAuthClientService.buildFrontendErrorRedirectUrl(any(), any()))
                .willReturn("http://localhost:5173/oauth/callback?error=invalid_state");

        mockMvc.perform(get("/api/auth/kakao/callback?code=sample_code"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("POST /api/auth/token/exchange 요청은 인증 없이 permitAll 처리되어 401(UNAUTHORIZED)이 발생하지 않는다")
    void exchangeToken_permitAll_notUnauthorized() throws Exception {
        mockMvc.perform(post("/api/auth/token/exchange")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"sample_code\"}"))
                .andExpect(result -> org.assertj.core.api.Assertions.assertThat(result.getResponse().getStatus()).isNotEqualTo(401));
    }

    @Test
    @DisplayName("GET /api/users/verify-nickname 요청은 인증이 필요해 토큰 없이 호출하면 401(UNAUTHORIZED)이 발생한다")
    void verifyNickname_requiresAuthentication_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/users/verify-nickname").param("nickname", "김뮤즈"))
                .andExpect(status().isUnauthorized());
    }
}
