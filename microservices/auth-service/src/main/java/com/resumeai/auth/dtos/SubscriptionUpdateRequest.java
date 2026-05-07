package com.resumeai.auth.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionUpdateRequest {
    private Long userId;
    private String planType;
    private String paymentStatus;
    private String paymentId;
}
