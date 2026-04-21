package com.resumeai.payment_service.config;

import java.util.*;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.paypal.base.rest.APIContext;
import com.paypal.base.rest.OAuthTokenCredential;
import com.paypal.base.rest.PayPalRESTException;


@Configuration
public class PayPalConfig {
	@Value("${paypal.client-id}")
	private String client_id;
	@Value("${paypal.client-secret}")
	private String client_secret;
	
	@Value("${paypal.mode}")
	private String mode;

	/*
	 * PayPal has two modes-> [SandBox: for testing] & [Live: for real production]
	 * So: Modes determinations required Whether transaction for testing or live 
	 */
	@Bean
    Map<String, String> paypalSdkConfig() {
        Map<String, String> configMap = new HashMap<>();
        configMap.put("mode", mode);
        return configMap;
    }
	
	/*
	 * It is use to generate the access token
	 * Look: PayPal uses OAuth 2.0
	 * OAuthTokenCredential is final class is used internally by the SDK to fetch access token for authenticated API calls
	 */
	@Bean
    OAuthTokenCredential oAuthTokenCredential() {
        return new OAuthTokenCredential(client_id, client_secret, paypalSdkConfig());
    }
	
	/*
	 * This is the core object used in every PayPal API calls
	 * it contains: [Credentials | Environment | request meta data]
	 */
	@Bean
    APIContext apiContext() throws PayPalRESTException {
        APIContext context = new APIContext(client_id, client_secret, mode);
        return context;
    }
}