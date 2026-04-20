package com.resumeai.aiservice.exception;

public class AiProviderException extends RuntimeException {
	
	private static final long serialVersionUID = 1L;

	public AiProviderException(String message) {
		super(message);
	}

	public AiProviderException(String message, Throwable cause) {
		super(message, cause);
	}
}

