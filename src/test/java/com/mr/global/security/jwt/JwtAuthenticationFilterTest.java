package com.mr.global.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@ExtendWith(OutputCaptureExtension.class)
class JwtAuthenticationFilterTest {

    @Test
    void invalidTokenLog_containsReasonAndRequestButNotToken(CapturedOutput output) throws Exception {
        JwtTokenProvider tokenProvider = mock(JwtTokenProvider.class);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(tokenProvider);
        FilterChain filterChain = mock(FilterChain.class);
        MockHttpServletRequest request = new MockHttpServletRequest("PATCH", "/api/users/me");
        MockHttpServletResponse response = new MockHttpServletResponse();
        String token = "sensitive.jwt.token";
        request.addHeader(JwtAuthenticationFilter.AUTHORIZATION_HEADER, "Bearer " + token);
        when(tokenProvider.validateAccessTokenResult(token)).thenReturn(JwtValidationResult.INVALID_SIGNATURE);

        filter.doFilter(request, response, filterChain);

        assertThat(output).contains("reason=INVALID_SIGNATURE", "method=PATCH", "uri=/api/users/me");
        assertThat(output).doesNotContain(token, "Bearer " + token);
        assertThat(request.getAttribute("exception")).isInstanceOf(io.jsonwebtoken.JwtException.class);
        verify(filterChain).doFilter(request, response);
    }
}
