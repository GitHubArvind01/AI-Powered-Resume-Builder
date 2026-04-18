package com.resumeai.resume_section_service.exception;

public class SectionServiceException extends RuntimeException {

    public SectionServiceException(String message) {
        super(message);
    }

    public SectionServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}

