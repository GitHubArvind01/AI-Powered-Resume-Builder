package com.resumeai.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.resumeai.auth.exception.JwtAuthenticationEntryPoint;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private final JwtAuthenticationFilter jwtAuthenticationFilter;
	private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http.csrf(csrf -> csrf.disable()) // - THIS IS KEY!
				.authorizeHttpRequests(authz -> authz
						.requestMatchers("/api/v1/auth/**", "/swagger-ui/**", "/h2-console/**", "/v3/api-docs/**", "/actuator/**").permitAll()
						.requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
						.anyRequest().authenticated());
		http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
		http.exceptionHandling(exception -> exception.authenticationEntryPoint(jwtAuthenticationEntryPoint));
		http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
		// For H2 Console
		http.headers(headers -> headers.frameOptions(frame -> frame.disable()));
		return http.build();
	}
}
