package com.resumeai.auth.exception;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import jakarta.servlet.http.HttpServletRequest;

@ControllerAdvice
public class GlobalExceptionHandler {
	
	@ExceptionHandler(RuntimeException.class)
	public ResponseEntity<ErrorResponse> handleRunTimeException(RuntimeException e, HttpServletRequest request){
		ErrorResponse errro = new ErrorResponse();
		errro.setDateTime(LocalDateTime.now());
		errro.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
		errro.setError(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());
		errro.setMessage(e.getMessage());
		errro.setPath(request.getRequestURL().toString());
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errro);
	}
	
	@ExceptionHandler(UnauthorizedException.class)
	public ResponseEntity<ErrorResponse> handleUnauthorizedException(UnauthorizedException e, HttpServletRequest request){
		ErrorResponse errro = new ErrorResponse();
		errro.setDateTime(LocalDateTime.now());
		errro.setStatus(HttpStatus.UNAUTHORIZED.value());
		errro.setError("Unauthorized access!");
		errro.setMessage(e.getMessage());
		errro.setPath(request.getRequestURL().toString());
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errro);
	}

	@ExceptionHandler(ForbiddenException.class)
	public ResponseEntity<ErrorResponse> handleForbiddenException(ForbiddenException e, HttpServletRequest request){
		ErrorResponse errro = new ErrorResponse();
		errro.setDateTime(LocalDateTime.now());
		errro.setStatus(HttpStatus.FORBIDDEN.value());
		errro.setError(HttpStatus.FORBIDDEN.getReasonPhrase());
		errro.setMessage(e.getMessage());
		errro.setPath(request.getRequestURL().toString());
		return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errro);
	}

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleNotFoundException(ResourceNotFoundException e, HttpServletRequest request){
		ErrorResponse errro = new ErrorResponse();
		errro.setDateTime(LocalDateTime.now());
		errro.setStatus(HttpStatus.NOT_FOUND.value());
		errro.setError(HttpStatus.NOT_FOUND.getReasonPhrase());
		errro.setMessage(e.getMessage());
		errro.setPath(request.getRequestURL().toString());
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errro);
	}

	@ExceptionHandler(BadRequestException.class)
	public ResponseEntity<ErrorResponse> handleBadRequestException(BadRequestException e, HttpServletRequest request){
		ErrorResponse errro = new ErrorResponse();
		errro.setDateTime(LocalDateTime.now());
		errro.setStatus(HttpStatus.BAD_REQUEST.value());
		errro.setError(HttpStatus.BAD_REQUEST.getReasonPhrase());
		errro.setMessage(e.getMessage());
		errro.setPath(request.getRequestURL().toString());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errro);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e, HttpServletRequest request){
		ErrorResponse errro = new ErrorResponse();
		errro.setDateTime(LocalDateTime.now());
		errro.setStatus(HttpStatus.BAD_REQUEST.value());
		errro.setError(HttpStatus.BAD_REQUEST.getReasonPhrase());
		errro.setMessage(e.getBindingResult().getFieldErrors().stream()
				.findFirst()
				.map(error -> error.getDefaultMessage())
				.orElse("Validation failed"));
		errro.setPath(request.getRequestURL().toString());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errro);
	}
	
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handle(Exception e, HttpServletRequest request){
		ErrorResponse errro = new ErrorResponse();
		errro.setDateTime(LocalDateTime.now());
		errro.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
		errro.setError("Internal Server error!");
		errro.setMessage(e.getMessage());
		errro.setPath(request.getRequestURL().toString());
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errro);
	}
}
