package com.resumeai.payment_service.dto;

import com.resumeai.payment_service.entity.PaymentRecord;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * PaymentResponseDTO - Response payload for payment operations
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponseDTO {

    private UUID id;
    private Long userId;
    private String paymentId;
    private String payerId;
    private BigDecimal amount;
    private String currency;
    private String description;
    private String planType;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Convert PaymentRecord entity to DTO
     */
    public static PaymentResponseDTO fromEntity(PaymentRecord record) {
        return PaymentResponseDTO.builder()
                .id(record.getId())
                .userId(record.getUserId())
                .paymentId(record.getPaymentId())
                .payerId(record.getPayerId())
                .amount(record.getAmount())
                .currency(record.getCurrency())
                .description(record.getDescription())
                .planType(record.getPlanType())
                .status(record.getStatus().name())
                .createdAt(record.getCreatedAt())
                .updatedAt(record.getUpdatedAt())
                .build();
    }
}

