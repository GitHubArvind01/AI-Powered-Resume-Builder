package com.resumeai.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.resumeai.auth.dtos.AuthResponse;
import com.resumeai.auth.service.GoogleAuthService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/auth/")
public class GoogleAuthController {
	
	private final GoogleAuthService googleAuthService;

	@GetMapping("/callback")
	public ResponseEntity<AuthResponse> handleGoogleCallback(@RequestParam String code){
		return ResponseEntity.ok(googleAuthService.handleGoogleAuth(code));
	}
}