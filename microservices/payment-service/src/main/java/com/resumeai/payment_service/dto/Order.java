package com.resumeai.payment_service.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Order DTO - Request payload for payment creation
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Order {

    @NotNull(message = "Price cannot be null")
    @Positive(message = "Price must be greater than 0")
    private Double price;

    @NotBlank(message = "Currency cannot be blank")
    @Size(min = 1, max = 3, message = "Currency code must be between 1 and 3 characters")
    private String currency;

    @NotBlank(message = "Payment method cannot be blank")
    private String method;

    @NotBlank(message = "Intent cannot be blank")
    private String intent;

    @NotBlank(message = "Description cannot be blank")
    @Size(min = 3, max = 500, message = "Description must be between 3 and 500 characters")
    private String description;
}