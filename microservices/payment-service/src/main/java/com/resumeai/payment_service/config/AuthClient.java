package com.resumeai.payment_service.config;

import com.resumeai.payment_service.dto.AuthResponseDTO;
import com.resumeai.payment_service.dto.SubscriptionUpdateRequest;
import com.resumeai.payment_service.dto.UserResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "AUTH-SERVICE")
public interface AuthClient {

    // Matches @GetMapping("/id/{id}") in UserController
    @GetMapping("/api/v1/auth/id/{id}")
    ResponseEntity<UserResponseDTO> getUserById(@PathVariable("id") Long id);

    // Matches @PostMapping("/update-subscription") in UserController
    @PostMapping("/api/v1/auth/update-subscription")
    ResponseEntity<AuthResponseDTO> updateSubscription(@RequestBody SubscriptionUpdateRequest request);
}
