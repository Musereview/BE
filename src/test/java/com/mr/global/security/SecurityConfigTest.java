package com.mr.global.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mr.domain.auth.entity.enums.SocialType;
import com.mr.domain.auth.service.AuthService;
import com.mr.domain.auth.service.OAuthClientService;
import com.mr.domain.user.entity.User;
import com.mr.domain.user.exception.UserErrorStatus;
import com.mr.domain.user.repository.UserRepository;
import com.mr.global.security.jwt.JwtTokenProvider;
import java.util.Optional;
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

    private static final Long USER_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @MockBean
    private OAuthClientService oAuthClientService;

    @MockBean
    private AuthService authService;

    @MockBean
    private UserRepository userRepository;

    private String guestToken() {
        given(userRepository.findById(USER_ID))
                .willReturn(Optional.of(User.createFromOAuth("https://cdn.example.com/profile/default-profile.png")));
        return tokenProvider.createAccessToken(USER_ID);
    }

    private String studentToken() {
        User user = User.createFromOAuth("https://cdn.example.com/profile/default-profile.png");
        user.updateNickname("김뮤즈");
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
        return tokenProvider.createAccessToken(USER_ID);
    }

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

    @Test
    @DisplayName("형식이 잘못된 JWT로 보호 API를 요청하면 500이 아닌 401이 발생한다")
    void protectedApi_withMalformedToken_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/users/verify-nickname")
                        .param("nickname", "김뮤즈")
                        .header("Authorization", "Bearer forged.jwt.token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("ROLE_GUEST 토큰으로 비즈니스 API 요청 시 403(ONBOARDING_REQUIRED)이 발생한다")
    void protectedApi_withGuestRole_returnsOnboardingRequired() throws Exception {
        mockMvc.perform(get("/api/learnings/home")
                        .header("Authorization", "Bearer " + guestToken()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(UserErrorStatus.ONBOARDING_REQUIRED.getCode()));
    }

    @Test
    @DisplayName("ROLE_GUEST 토큰으로 온보딩 등록(POST /api/users/me/profile)은 통과한다")
    void onboardingApi_withGuestRole_isAllowed() throws Exception {
        mockMvc.perform(post("/api/users/me/profile")
                        .header("Authorization", "Bearer " + guestToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"김뮤즈\",\"skillLevel\":\"INTERMEDIATE\"}"))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotEqualTo(403));
    }

    @Test
    @DisplayName("ROLE_GUEST 토큰으로 프로필 조회(GET)/수정(PATCH)은 여전히 403으로 막힌다 (온보딩 전이라 값이 없어서 통과시키면 안 됨)")
    void profileGetAndPatch_withGuestRole_areBlocked() throws Exception {
        String guestToken = guestToken();

        mockMvc.perform(get("/api/users/me/profile")
                        .header("Authorization", "Bearer " + guestToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(UserErrorStatus.ONBOARDING_REQUIRED.getCode()));

        mockMvc.perform(patch("/api/users/me/profile")
                        .header("Authorization", "Bearer " + guestToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"새로운뮤즈\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(UserErrorStatus.ONBOARDING_REQUIRED.getCode()));
    }

    @Test
    @DisplayName("ROLE_STUDENT 토큰으로 비즈니스 API는 403/401 없이 정상 접근된다")
    void protectedApi_withStudentRole_isAllowed() throws Exception {
        mockMvc.perform(get("/api/learnings/home")
                        .header("Authorization", "Bearer " + studentToken()))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotIn(401, 403));
    }

    @Test
    @DisplayName("미인증 사용자가 온보딩 API(POST /api/users/me/profile)에 접근하면 401이 발생한다")
    void onboardingApi_withoutToken_returnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/users/me/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"김뮤즈\",\"skillLevel\":\"INTERMEDIATE\"}"))
                .andExpect(status().isUnauthorized());
    }
}
