package com.resumeai.aiservice.exception;

public class QuotaExceededException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public QuotaExceededException(String message) {
		super(message);
	}

	public QuotaExceededException(String message, Throwable cause) {
		super(message, cause);
	}
}

