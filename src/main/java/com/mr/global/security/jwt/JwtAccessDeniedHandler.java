package com.mr.global.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mr.domain.user.entity.enums.UserRole;
import com.mr.domain.user.exception.UserErrorStatus;
import com.mr.global.apipayload.ApiResponse;
import com.mr.global.apipayload.code.CommonStatus;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isGuest = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(UserRole.ROLE_GUEST.getKey()));

        ApiResponse<Object> apiResponse;
        if (isGuest) {
            apiResponse = ApiResponse.onFailure(
                    UserErrorStatus.ONBOARDING_REQUIRED.getCode(),
                    UserErrorStatus.ONBOARDING_REQUIRED.getMessage(),
                    null
            );
        } else {
            apiResponse = ApiResponse.onFailure(
                    CommonStatus.FORBIDDEN.getCode(),
                    CommonStatus.FORBIDDEN.getMessage(),
                    null
            );
        }

        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }
}