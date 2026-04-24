package com.resumeai.notification_service.service;

import com.resumeai.notification_service.dto.EmailEvent;

/**
 * Contract for email sending operations inside Notification Service.
 */
public interface EmailService {

    /**
     * Sends an OTP email based on the incoming {@link EmailEvent}.
     *
     * @param event the email event containing recipient, OTP, subject and purpose
     */
    void sendOtpEmail(EmailEvent event);
}
