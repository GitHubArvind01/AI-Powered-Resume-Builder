package com.resumeai.aiservice.client;

public interface AiProviderClient {

	/**
	 * Call the AI provider with a prompt
	 * 
	 * @param prompt The input prompt
	 * @return The AI response
	 * @throws Exception if the API call fails
	 */
	String callAiProvider(String prompt) throws Exception;

	/**
	 * Get the model name
	 * 
	 * @return The model identifiers
	 */
	String getModelName();

	/**
	 * Check if the provider is available
	 * 
	 * @return true if available, false otherwise
	 */
	boolean isAvailable();
}

