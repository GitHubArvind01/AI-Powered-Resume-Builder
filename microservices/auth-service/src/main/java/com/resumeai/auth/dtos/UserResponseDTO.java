package com.resumeai.auth.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserResponseDTO {
    private Long id;
 	private String fullName;
    private String email;
    private String phone;
    private String role;
    private boolean isActive;
    private String subscriptionPlan;
    private Boolean premiumActive;
    private String subscriptionStatus;
    private String paymentStatus;
    private String lastPaymentId;
    private String subscriptionStartDate;
    private String subscriptionEndDate;
}
