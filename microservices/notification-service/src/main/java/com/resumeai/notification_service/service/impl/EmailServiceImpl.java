package com.resumeai.notification_service.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.resumeai.notification_service.dto.EmailEvent;
import com.resumeai.notification_service.service.EmailService;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;

/**
 * Sends HTML OTP emails via JavaMailSender (Gmail SMTP).
 * <p>
 * This implementation handles three purposes:
 * <ul>
 *   <li>REGISTER        — new user account verification</li>
 *   <li>FORGOT_PASSWORD — password reset flow</li>
 *   <li>UPDATE_EMAIL    — email address change verification</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    private final JavaMailSender mailSender;

    @Override
    public void sendOtpEmail(EmailEvent event) {
        log.info("[NOTIFICATION] Preparing OTP email → to={}, purpose={}", event.getTo(), event.getPurpose());

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(event.getTo());
            helper.setSubject("🔐 " + event.getSubject());

            String messageText = resolveBodyText(event.getPurpose());
            String htmlContent = buildHtmlTemplate(messageText, event.getOtp());

            helper.setText(htmlContent, true); // true = HTML enabled
            mailSender.send(message);

            log.info("[NOTIFICATION] Email sent successfully → to={}, purpose={}", event.getTo(), event.getPurpose());

        } catch (MessagingException ex) {
            log.error("[NOTIFICATION] Failed to send email → to={}, purpose={}, error={}",
                    event.getTo(), event.getPurpose(), ex.getMessage(), ex);
            // Do NOT re-throw — consumer should not crash on a single bad email.
            // In production, you could push to a dead-letter queue here.
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private String resolveBodyText(String purpose) {
        return switch (purpose) {
            case "REGISTER"        -> "Use this OTP to complete your registration. Valid for 5 minutes.";
            case "FORGOT_PASSWORD" -> "Use the following code to reset your password. Valid for 5 minutes.";
            case "UPDATE_EMAIL"    -> "Use this code to verify your new email address. Valid for 5 minutes.";
            default                -> "Use this OTP for verification. Valid for 5 minutes.";
        };
    }

    private String buildHtmlTemplate(String messageText, String otp) {
        return """
                <div style="font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                            background-color: #f4f7f6; padding: 40px; border-radius: 10px;">
                  <div style="max-width: 520px; margin: auto; background-color: #ffffff;
                              border-radius: 10px; overflow: hidden;
                              box-shadow: 0 4px 16px rgba(0,0,0,0.1);">

                    <!-- Header -->
                    <div style="background: linear-gradient(135deg, #1a1a2e 0%%, #16213e 50%%, #0f3460 100%%);
                                padding: 28px; text-align: center;">
                      <h1 style="color: #e94560; margin: 0; font-size: 22px; letter-spacing: 3px;
                                 text-transform: uppercase;">AI Resume Builder</h1>
                      <p style="color: #a0aec0; margin: 6px 0 0; font-size: 13px; letter-spacing: 1px;">
                        Secure Verification System
                      </p>
                    </div>

                    <!-- Body -->
                    <div style="padding: 40px; text-align: center;">
                      <p style="color: #4a5568; font-size: 16px; margin: 0 0 8px;">Hello,</p>
                      <p style="color: #4a5568; font-size: 15px; margin: 0 0 30px;">%s</p>

                      <!-- OTP Box -->
                      <div style="margin: 0 auto 30px; display: inline-block;">
                        <span style="font-size: 38px; font-weight: 800; color: #0f3460;
                                     letter-spacing: 10px; padding: 14px 28px;
                                     border: 2px dashed #e94560; border-radius: 8px;
                                     background-color: #f7f9fc; display: inline-block;">
                          %s
                        </span>
                      </div>

                      <p style="color: #a0aec0; font-size: 13px; margin: 0;">
                        ⚠️ This code expires in <strong>5 minutes</strong>.
                        If you did not request this, please ignore this email.
                      </p>
                    </div>

                    <!-- Footer -->
                    <div style="background-color: #f8f9fa; padding: 20px;
                                text-align: center; border-top: 1px solid #edf2f7;">
                      <p style="color: #cbd5e0; font-size: 12px; margin: 0;">
                        © 2026 AI-Powered Resume Builder · All rights reserved
                      </p>
                    </div>

                  </div>
                </div>
                """.formatted(messageText, otp);
    }
}
