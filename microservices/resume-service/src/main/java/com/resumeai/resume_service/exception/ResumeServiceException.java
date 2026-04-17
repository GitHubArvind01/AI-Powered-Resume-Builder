package com.resumeai.resume_service.exception;

public class ResumeServiceException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ResumeServiceException(String message) {
        super(message);
    }

    public ResumeServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}

