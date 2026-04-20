package com.resumeai.aiservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Configuration
@ConfigurationProperties(prefix = "ai.provider")
@Data
public class AiProviderConfig {

	private GeminiConfig gemini;
	private QuotaConfig quota;

	@Data
	public static class GeminiConfig {
		private String apiKey;
		private String model;
		private Integer maxTokens;
	}

	@Data
	public static class QuotaConfig {
		private Integer freeTierMonthlyLimit;
	}
}

