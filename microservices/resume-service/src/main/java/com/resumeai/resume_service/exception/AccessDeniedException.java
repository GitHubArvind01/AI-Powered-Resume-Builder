package com.resumeai.resume_service.exception;

/**
 * Exception thrown when access is denied or ownership verification fails
 * Used when a user tries to access/modify a resume they don't own
 */
public class AccessDeniedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public AccessDeniedException(String message) {
        super(message);
    }

    public AccessDeniedException(String message, Throwable cause) {
        super(message, cause);
    }
}

