package com.mr.global.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mr.domain.auth.service.AuthService;
import com.mr.domain.auth.service.OAuthClientService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
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
    @DisplayName("GET /api/auth/login/KAKAO 요청은 인증 없이 permitAll 처리되어 401(COMMON_401_01)이 발생하지 않는다")
    void startOAuthLogin_permitAll_notUnauthorized() throws Exception {
        mockMvc.perform(get("/api/auth/login/KAKAO"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    org.assertj.core.api.Assertions.assertThat(status).isNotEqualTo(401);
                });
    }

    @Test
    @DisplayName("GET /api/auth/kakao/callback 요청은 인증 없이 permitAll 처리되어 401(COMMON_401_01)이 발생하지 않는다")
    void oAuthCallback_permitAll_notUnauthorized() throws Exception {
        mockMvc.perform(get("/api/auth/kakao/callback?code=sample_code"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    org.assertj.core.api.Assertions.assertThat(status).isNotEqualTo(401);
                });
    }
}
