package com.resumeai.auth.dtos;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RegisterRequest {
	@NotNull(message = "userName null not allowed")
	private String username;
	@NotNull(message = "email null not allowed")
    private String email;
	@NotNull(message = "password null not allowed")
    private String password;
	@NotNull(message = "role null not allowed")
    private String role;
}
