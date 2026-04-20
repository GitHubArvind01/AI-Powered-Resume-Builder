package com.resumeai.aiservice.exception;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.resumeai.aiservice.dto.ErrorResponse;

import jakarta.servlet.http.HttpServletRequest;

@ControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException e,
			HttpServletRequest request) {
		ErrorResponse error = new ErrorResponse();
		error.setTimestamp(LocalDateTime.now());
		error.setStatus(HttpStatus.NOT_FOUND.value());
		error.setError("Resource Not Found");
		error.setMessage(e.getMessage());
		error.setPath(request.getRequestURL().toString());
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
	}

	@ExceptionHandler(QuotaExceededException.class)
	public ResponseEntity<ErrorResponse> handleQuotaExceeded(QuotaExceededException e,
			HttpServletRequest request) {
		ErrorResponse error = new ErrorResponse();
		error.setTimestamp(LocalDateTime.now());
		error.setStatus(HttpStatus.PAYMENT_REQUIRED.value());
		error.setError("Quota Exceeded");
		error.setMessage(e.getMessage());
		error.setPath(request.getRequestURL().toString());
		return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(error);
	}

	@ExceptionHandler(AiProviderException.class)
	public ResponseEntity<ErrorResponse> handleAiProviderException(AiProviderException e,
			HttpServletRequest request) {
		ErrorResponse error = new ErrorResponse();
		error.setTimestamp(LocalDateTime.now());
		error.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
		error.setError("AI Service Unavailable");
		error.setMessage(e.getMessage());
		error.setPath(request.getRequestURL().toString());
		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
	}

	@ExceptionHandler(RuntimeException.class)
	public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException e,
			HttpServletRequest request) {
		ErrorResponse error = new ErrorResponse();
		error.setTimestamp(LocalDateTime.now());
		error.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
		error.setError("Internal Server Error");
		error.setMessage(e.getMessage());
		error.setPath(request.getRequestURL().toString());
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleGenericException(Exception e, HttpServletRequest request) {
		ErrorResponse error = new ErrorResponse();
		error.setTimestamp(LocalDateTime.now());
		error.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
		error.setError("Internal Server Error");
		error.setMessage(e.getMessage());
		error.setPath(request.getRequestURL().toString());
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
	}
}

