package com.mr.global.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mr.global.apipayload.ApiResponse;
import com.mr.global.apipayload.code.CommonStatus;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
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

        // 403 Forbidden 공통 JSON 응답 반환
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);

        ApiResponse<Object> apiResponse = ApiResponse.onFailure(
                CommonStatus.FORBIDDEN.getCode(),
                CommonStatus.FORBIDDEN.getMessage(),
                null
        );

        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }
}