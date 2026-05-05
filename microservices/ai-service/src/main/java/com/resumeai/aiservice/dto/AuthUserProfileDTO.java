package com.resumeai.aiservice.dto;

import lombok.Data;

@Data
public class AuthUserProfileDTO {
	private String fullName;
	private String email;
	private String phone;
	private String role;
	private boolean active;
	private String subscriptionPlan;
}
