package com.resumeai.aiservice.client;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import com.resumeai.aiservice.config.AiProviderConfig;
import com.resumeai.aiservice.exception.AiProviderException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
@Slf4j
public class GeminiClient implements AiProviderClient {

	private final AiProviderConfig config;
	private Client client;

	public GeminiClient(AiProviderConfig config) {
		this.config = config;
	}

	@PostConstruct
	public void init() {
		if (isKeyPresent()) {
			// Initializes the client using the API Key from your Configs
			this.client = Client.builder()
					.apiKey(config.getGemini().getApiKey())
					.build();
			log.info("Gemini Client successfully initialized with model: {}", config.getGemini().getModel());
		}
	}

	@Retryable(
			value = { Exception.class },
			maxAttempts = 3, // It will try up to 3 times
			backoff = @Backoff(delay = 2000, multiplier = 2.0) // Waits 2 seconds, then 4 seconds between tries
	)
	@Override
	public String callAiProvider(String prompt) throws Exception {
		if (!isKeyPresent()) {
			log.warn("Gemini API key missing - returning mock response");
			return "Mock Response: Focus on Java, Spring Boot, and Microservices. [Key not configured]";
		}

		try {
			String model = config.getGemini().getModel();

			// Official SDK method call
			GenerateContentResponse response = client.models.generateContent(model, prompt, null);

			if (response != null && response.text() != null) {
				return response.text();
			}

			throw new AiProviderException("Gemini returned an empty response.");
		} catch (Exception e) {
			log.error("Gemini API Error: {}", e.getMessage());
			throw new AiProviderException("Failed to call Gemini: " + e.getMessage(), e);
		}
	}

	@Override
	public String getModelName() {
		return config.getGemini().getModel();
	}

	@Override
	public boolean isAvailable() {
		return isKeyPresent();
	}

	private boolean isKeyPresent() {
		return config.getGemini() != null &&
				config.getGemini().getApiKey() != null &&
				!config.getGemini().getApiKey().isBlank();
	}
}