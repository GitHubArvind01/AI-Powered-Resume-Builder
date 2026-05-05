package com.resumeai.aiservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.resumeai.aiservice.dto.AuthUserProfileDTO;

@FeignClient(name = "AUTH-SERVICE")
public interface AuthUserClient {

	@GetMapping("/api/v1/auth/id/{id}")
	AuthUserProfileDTO getUserById(@PathVariable("id") Long id);
}
