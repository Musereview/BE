package com.mr.global.security;

import com.mr.global.security.jwt.JwtAccessDeniedHandler;
import com.mr.global.security.jwt.JwtAuthenticationEntryPoint;
import com.mr.global.security.jwt.JwtAuthenticationFilter;
import com.mr.global.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
            "/api/auth/*/callback",
            "/api/auth/reissue",
            "/api/auth/token/exchange"
    };

    /**
     * AntPathRequestMatcher를 명시적으로 사용하는 이유:
     * Spring Security 6 환경에서와일드카드 패턴 경로(/swagger-ui/**, /api/auth/** 등)에 대해
     * Ant 매칭 규칙을 명시적으로 지정하여 URL 패턴 정규화 모호성을 방지하기 위함입니다.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(jwtAccessDeniedHandler)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher("/swagger-ui/**"),
                                org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher("/v3/api-docs/**"),
                                org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher("/api/auth/login/**"),
                                org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher("/api/auth/**/callback"),
                                org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher("/api/auth/*/callback"),
                                org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher("/api/auth/reissue"),
                                org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher("/api/auth/token/exchange")
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // CORS 설정
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOriginPatterns(List.of(
                "http://localhost:3000",
                "http://localhost:5173",
                "https://*.musereview.site"
        ));

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