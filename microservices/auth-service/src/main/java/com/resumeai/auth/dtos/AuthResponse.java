package com.resumeai.auth.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
	 private String token;
	 private String role;
	 private String subscriptionPlan;
	 private String message;
}
