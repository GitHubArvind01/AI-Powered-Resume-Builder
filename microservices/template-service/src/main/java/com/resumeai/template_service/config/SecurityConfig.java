package com.resumeai.template_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security Configuration for Template Service
 *
 * Permits public access to:
 * - Swagger UI and API documentation
 * - H2 console (for development)
 * - Actuator endpoints (health checks)
 * - API endpoints (for microservice communication)
 *
 * CSRF is disabled for development and internal service communication.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable()) // Disable CSRF for development and microservice communication
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers(
                                "/api/v1/**",           // API endpoints
                                "/swagger-ui/**",       // Swagger UI
                                "/v3/api-docs/**",      // OpenAPI documentation
                                "/h2-console/**",       // H2 database console
                                "/actuator/**"          // Actuator endpoints
                        ).permitAll()
                        .anyRequest().permitAll());

        // For H2 Console - disable frame options to allow H2 console in iframe
        http.headers(headers -> headers.frameOptions(frame -> frame.disable()));

        return http.build();
    }
}
