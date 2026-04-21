package com.resumeai.payment_service;

import com.paypal.base.rest.APIContext;
import com.paypal.base.rest.OAuthTokenCredential;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.HashMap;
import java.util.Map;

/**
 * Test configuration for Payment Service tests
 * Provides mock PayPal beans to avoid environment variable dependencies
 */
@TestConfiguration
public class TestConfig {

    @Bean
    @Primary
    public APIContext apiContext() {
        return new APIContext("test-client-id", "test-client-secret", "sandbox");
    }

    @Bean
    @Primary
    public OAuthTokenCredential oAuthTokenCredential() {
        Map<String, String> configMap = new HashMap<>();
        configMap.put("mode", "sandbox");
        return new OAuthTokenCredential("test-client-id", "test-client-secret", configMap);
    }

    @Bean
    @Primary
    public Map<String, String> paypalSdkConfig() {
        Map<String, String> configMap = new HashMap<>();
        configMap.put("mode", "sandbox");
        return configMap;
    }
}

