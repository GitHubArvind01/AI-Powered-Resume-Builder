package com.resumeai.payment_service.dto;
import lombok.Data;

@Data
public class UserResponseDTO {
    private Long id;
    private String email;
    private String fullName;
    private String subscriptionPlan;
}