package com.mr.global.security.jwt;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider tokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String jwt = resolveToken(request);

        if (StringUtils.hasText(jwt)) {
            JwtValidationResult validationResult = tokenProvider.validateAccessTokenResult(jwt);
            if (validationResult != JwtValidationResult.VALID) {
                log.warn("JWT validation failed: reason={}, method={}, uri={}",
                        validationResult, request.getMethod(), request.getRequestURI());
                SecurityContextHolder.clearContext();
                request.setAttribute("exception", new JwtException("유효하지 않거나 만료된 토큰입니다."));
            } else {
                try {
                    Authentication authentication = tokenProvider.getAuthentication(jwt);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } catch (UsernameNotFoundException | NumberFormatException e) {
                    log.error("Security Context에 인증 정보를 저장할 수 없습니다. Error: {}", e.getMessage());
                    SecurityContextHolder.clearContext();
                    request.setAttribute("exception", e);
                } catch (Exception e) {
                    log.error("JWT 인증 처리 중 알 수 없는 에러 발생: {}", e.getMessage());
                    SecurityContextHolder.clearContext();
                    request.setAttribute("exception", e);
                }
            }
        }

        filterChain.doFilter(request, response);
    }
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
