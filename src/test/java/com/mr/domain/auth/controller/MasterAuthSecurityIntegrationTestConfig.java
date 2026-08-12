package com.mr.domain.auth.controller;

import com.mr.domain.auth.config.MasterAuthSecurityConfig;
import com.mr.global.security.SecurityConfig;
import com.mr.global.security.jwt.JwtAccessDeniedHandler;
import com.mr.global.security.jwt.JwtAuthenticationEntryPoint;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Import;

@SpringBootConfiguration(proxyBeanMethods = false)
@EnableAutoConfiguration(exclude = {
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        JpaRepositoriesAutoConfiguration.class
})
@Import({
        SecurityConfig.class,
        MasterAuthSecurityConfig.class,
        MasterAuthController.class,
        JwtAuthenticationEntryPoint.class,
        JwtAccessDeniedHandler.class
})
class MasterAuthSecurityIntegrationTestConfig {
}
