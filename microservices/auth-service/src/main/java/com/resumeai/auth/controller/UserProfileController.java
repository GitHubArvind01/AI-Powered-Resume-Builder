package com.resumeai.auth.controller;

import com.resumeai.auth.dtos.CurrentUserResponseDTO;
import com.resumeai.auth.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User Profile", description = "Endpoints for authenticated user profile access")
public class UserProfileController {

    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "Get current user", description = "Returns the latest profile, role, and subscription for the authenticated user.")
    public ResponseEntity<CurrentUserResponseDTO> getCurrentUser(
            @RequestHeader("X-User-Email") String userEmail) {
        return ResponseEntity.ok(userService.getCurrentUser(userEmail));
    }
}
