package com.mr.global.security;

import com.mr.global.security.jwt.JwtAccessDeniedHandler;
import com.mr.global.security.jwt.JwtAuthenticationEntryPoint;
import com.mr.global.security.jwt.JwtAuthenticationFilter;
import com.mr.global.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;
import java.util.Map;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;

    private static final String[] PUBLIC_URLS = {
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/api/auth/login/**",
            "/api/auth/**/callback",
            "/api/auth/reissue",
            "/api/auth/token/exchange"
    };

    // 온보딩 미완료(ROLE_GUEST) 사용자에게도 허용할 (HTTP Method, URL) 조합.
    // /api/users/me/profile은 최초 등록(POST)만 허용 — GET/PATCH는 온보딩을 이미 마친
    // 사용자를 전제로 하는 동작이라 GUEST에게 열어두면 서비스 레이어에서 STUDENT_NOT_FOUND 등
    // 엉뚱한 오류로 이어진다.
    private static final Map<HttpMethod, String[]> ALLOWED_ONBOARDING_URLS = Map.of(
            HttpMethod.POST, new String[]{"/api/users/me/profile"},
            HttpMethod.GET, new String[]{"/api/users/verify-nickname"}
    );

    /**
     * AntPathRequestMatcher를 명시적으로 사용하는 이유:
     * Spring Security 6 환경에서 와일드카드 패턴 경로(/swagger-ui/**, /api/auth/** 등)에 대해
     * Ant 매칭 규칙을 명시적으로 지정하여 URL 패턴 정규화 모호성을 방지하기 위함입니다.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        org.springframework.security.web.util.matcher.AntPathRequestMatcher[] publicMatchers =
                java.util.Arrays.stream(PUBLIC_URLS)
                        .map(org.springframework.security.web.util.matcher.AntPathRequestMatcher::antMatcher)
                        .toArray(org.springframework.security.web.util.matcher.AntPathRequestMatcher[]::new);

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(jwtAccessDeniedHandler)
                )
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(publicMatchers).permitAll();
                    ALLOWED_ONBOARDING_URLS.forEach((method, urls) ->
                            auth.requestMatchers(method, urls).hasAnyRole("GUEST", "STUDENT", "TEACHER", "ADMIN"));
                    auth.anyRequest().hasAnyRole("STUDENT", "TEACHER", "ADMIN");
                })
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @org.springframework.beans.factory.annotation.Value("${cors.allowed-origins:http://localhost:3000,http://localhost:5173,https://musereview-sigma.vercel.app}")
    private String allowedOrigins;

    // CORS 설정
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        List<String> origins = java.util.Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();

        if (origins.isEmpty()) {
            throw new IllegalStateException("CORS allowed origins must not be empty");
        }
        configuration.setAllowedOrigins(origins);

        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));

        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With"));

        configuration.setExposedHeaders(List.of("Authorization"));

        configuration.setAllowCredentials(true);

        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}