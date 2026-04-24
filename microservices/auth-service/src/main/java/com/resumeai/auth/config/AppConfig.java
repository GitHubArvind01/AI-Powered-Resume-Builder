package com.resumeai.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.RestTemplate;

import lombok.AllArgsConstructor;

@Configuration
@AllArgsConstructor
public class AppConfig {
	@Bean
    RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    @LoadBalanced
    @Qualifier("loadBalancedRestTemplate")
    RestTemplate loadBalancedRestTemplate() {
        return new RestTemplate();
    }
	
    @Bean
    PasswordEncoder passwordEncoder() {
	    return new BCryptPasswordEncoder();
	}
}
