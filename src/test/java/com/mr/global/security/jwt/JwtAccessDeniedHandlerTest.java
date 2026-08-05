package com.mr.global.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mr.domain.user.entity.enums.UserRole;
import com.mr.domain.user.exception.UserErrorStatus;
import com.mr.global.apipayload.code.CommonStatus;
import com.mr.global.security.principal.CustomUserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAccessDeniedHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JwtAccessDeniedHandler accessDeniedHandler = new JwtAccessDeniedHandler(objectMapper);

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("ROLE_GUEST 유저가 접근 거부될 경우 ONBOARDING_REQUIRED 에러 응답을 반환한다")
    void handle_guestUser_returnsOnboardingRequired() throws Exception {
        CustomUserDetails guestUser = new CustomUserDetails(1L, UserRole.ROLE_GUEST);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(guestUser, "", guestUser.getAuthorities())
        );

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        accessDeniedHandler.handle(request, response, new AccessDeniedException("Access Denied"));

        assertThat(response.getStatus()).isEqualTo(403);
        String content = response.getContentAsString();
        assertThat(content).contains(UserErrorStatus.ONBOARDING_REQUIRED.getCode());
        assertThat(content).contains(UserErrorStatus.ONBOARDING_REQUIRED.getMessage());
    }

    @Test
    @DisplayName("GUEST가 아닌 유저가 접근 거부될 경우 FORBIDDEN 공통 에러 응답을 반환한다")
    void handle_nonGuestUser_returnsForbidden() throws Exception {
        CustomUserDetails studentUser = new CustomUserDetails(1L, UserRole.ROLE_STUDENT);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(studentUser, "", studentUser.getAuthorities())
        );

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        accessDeniedHandler.handle(request, response, new AccessDeniedException("Access Denied"));

        assertThat(response.getStatus()).isEqualTo(403);
        String content = response.getContentAsString();
        assertThat(content).contains(CommonStatus.FORBIDDEN.getCode());
    }
}
