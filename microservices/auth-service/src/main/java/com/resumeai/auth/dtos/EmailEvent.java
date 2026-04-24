package com.resumeai.auth.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * EmailEvent is published by Auth Service and consumed by Notification Service.
 * <p>
 * purpose values: REGISTER | FORGOT_PASSWORD | UPDATE_EMAIL
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailEvent {

    /** Recipient email address */
    private String to;

    /** OTP code to embed in the email body */
    private String otp;

    /** Email subject line */
    private String subject;

    /**
     * Describes why the email is being sent.
     * Allowed values: REGISTER, FORGOT_PASSWORD, UPDATE_EMAIL
     */
    private String purpose;
}
