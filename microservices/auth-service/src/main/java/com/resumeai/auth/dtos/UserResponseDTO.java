package com.resumeai.auth.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserResponseDTO {
 	private String fullName;
    private String email;
    private String phone;
    private String role;
    private boolean isActive;
    private String subscriptionPlan;
}