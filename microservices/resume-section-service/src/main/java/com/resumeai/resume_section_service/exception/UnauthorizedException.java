package com.resumeai.resume_section_service.exception;

/**
 * Exception thrown when a user attempts to perform an operation they are not authorized to perform.
 * For example: accessing or modifying a resume section that does not belong to them.
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }

    public UnauthorizedException(String message, Throwable cause) {
        super(message, cause);
    }
}

