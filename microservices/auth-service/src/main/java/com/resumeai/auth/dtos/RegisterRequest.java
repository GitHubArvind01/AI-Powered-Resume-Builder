package com.resumeai.auth.dtos;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RegisterRequest {
	@NotNull(message = "Name null not allowed")
	private String fullName;
	@NotNull(message = "email null not allowed")
    private String email;
	@NotNull(message = "password null not allowed")
    private String password;
	@NotNull(message = "phone null not allowed")
    private String phone;
}
