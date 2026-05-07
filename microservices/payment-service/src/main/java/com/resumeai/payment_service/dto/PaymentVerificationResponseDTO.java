package com.resumeai.payment_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentVerificationResponseDTO {
    private boolean success;
    private String message;
    private String token;
    private CurrentUserResponseDTO user;
    private PaymentResponseDTO payment;
}
