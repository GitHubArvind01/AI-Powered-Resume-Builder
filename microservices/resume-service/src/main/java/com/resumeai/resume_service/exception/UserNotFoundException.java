package com.resumeai.resume_service.exception;

/**
 * Exception thrown when a user is not found or doesn't exist
 * Used for cross-service validation when auth-service doesn't recognize a user
 */
public class UserNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public UserNotFoundException(String message) {
        super(message);
    }

    public UserNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}

