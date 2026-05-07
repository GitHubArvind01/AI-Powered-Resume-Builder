package com.resumeai.payment_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CurrentUserResponseDTO {
    private Long id;
    private String fullName;
    private String email;
    private String phone;
    private String role;
    private boolean active;
    private String subscriptionPlan;
    private Boolean premiumActive;
    private String subscriptionStatus;
    private String paymentStatus;
    private String lastPaymentId;
    private String subscriptionStartDate;
    private String subscriptionEndDate;
}
