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

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    private final JavaMailSender mailSender;

    @Override
    public void sendOtpEmail(EmailEvent event) {
        log.info("[NOTIFICATION] Preparing email to={} purpose={}", event.getTo(), event.getPurpose());

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(event.getTo());
            helper.setSubject(event.getSubject());

            String bodyText = resolveBodyText(event.getPurpose(), event.getMessage());
            String htmlContent = isOtpPurpose(event.getPurpose())
                    ? buildOtpHtmlTemplate(bodyText, event.getOtp())
                    : buildMessageHtmlTemplate(bodyText);

            helper.setText(htmlContent, true);
            mailSender.send(message);

            log.info("[NOTIFICATION] Email sent successfully to={} purpose={}", event.getTo(), event.getPurpose());
        } catch (MessagingException ex) {
            log.error("[NOTIFICATION] Failed to send email to={} purpose={} error={}",
                    event.getTo(), event.getPurpose(), ex.getMessage(), ex);
        }
    }

    private boolean isOtpPurpose(String purpose) {
        return "REGISTER".equals(purpose)
                || "FORGOT_PASSWORD".equals(purpose)
                || "UPDATE_EMAIL".equals(purpose);
    }

    private String resolveBodyText(String purpose, String eventMessage) {
        return switch (purpose) {
            case "REGISTER" -> "Use this OTP to complete your registration. Valid for 5 minutes.";
            case "FORGOT_PASSWORD" -> "Use the following code to reset your password. Valid for 5 minutes.";
            case "UPDATE_EMAIL" -> "Use this code to verify your new email address. Valid for 5 minutes.";
            case "USER_ACTIVATED" -> eventMessage != null
                    ? eventMessage
                    : "Great news! Your account has been activated. You can now log in and access all platform features.";
            case "USER_DELETED", "USER_DEACTIVATED" -> eventMessage != null
                    ? eventMessage
                    : "An update has been made to your account.";
            default -> eventMessage != null ? eventMessage : "Use this OTP for verification. Valid for 5 minutes.";
        };
    }

    private String buildOtpHtmlTemplate(String messageText, String otp) {
        return """
        <div style="margin:0;padding:0;background-color:#f4f7f6;">
          <table width="100%%" cellpadding="0" cellspacing="0" border="0"
                 style="font-family:Segoe UI,Arial,sans-serif;">
            <tr>
              <td align="center" style="padding:20px 10px;">
                
                <table width="100%%" cellpadding="0" cellspacing="0" border="0"
                       style="max-width:480px;background:#ffffff;border-radius:10px;
                              overflow:hidden;box-shadow:0 4px 12px rgba(0,0,0,0.1);">

                  <!-- HEADER -->
                  <tr>
                    <td style="background:#0f3460;padding:20px;text-align:center;">
                      <h1 style="color:#e94560;margin:0;font-size:18px;">
                        AI Resume Builder
                      </h1>
                      <p style="color:#cbd5e0;font-size:12px;margin:5px 0 0;">
                        Secure Verification
                      </p>
                    </td>
                  </tr>

                  <!-- BODY -->
                  <tr>
                    <td style="padding:20px;text-align:center;">
                      <p style="font-size:15px;color:#4a5568;margin:0 0 10px;">
                        Hello,
                      </p>

                      <p style="font-size:14px;color:#4a5568;margin:0 0 20px;">
                        %s
                      </p>

                      <!-- OTP BOX -->
                      <div style="margin:20px 0;">
                        <span style="display:inline-block;
                                     font-size:28px;
                                     font-weight:bold;
                                     color:#0f3460;
                                     letter-spacing:4px;
                                     padding:12px 16px;
                                     border:2px dashed #e94560;
                                     border-radius:6px;
                                     background:#f7f9fc;
                                     word-break:break-all;">
                          %s
                        </span>
                      </div>

                      <p style="font-size:12px;color:#718096;margin-top:10px;">
                        This code expires in <strong>5 minutes</strong>.
                      </p>
                    </td>
                  </tr>

                  <!-- FOOTER -->
                  <tr>
                    <td style="background:#f8f9fa;padding:15px;text-align:center;">
                      <p style="font-size:11px;color:#a0aec0;margin:0;">
                        © 2026 AI Resume Builder
                      </p>
                    </td>
                  </tr>

                </table>

              </td>
            </tr>
          </table>
        </div>
        """.formatted(messageText, otp != null ? otp : "");
    }
    private String buildMessageHtmlTemplate(String messageText) {
        return """
                <div style="font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                            background-color: #f4f7f6; padding: 40px; border-radius: 10px;">
                  <div style="max-width: 520px; margin: auto; background-color: #ffffff;
                              border-radius: 10px; overflow: hidden;
                              box-shadow: 0 4px 16px rgba(0,0,0,0.1);">
                    <div style="background: linear-gradient(135deg, #1a1a2e 0%%, #16213e 50%%, #0f3460 100%%);
                                padding: 28px; text-align: center;">
                      <h1 style="color: #e94560; margin: 0; font-size: 22px; letter-spacing: 3px;
                                 text-transform: uppercase;">AI Resume Builder</h1>
                      <p style="color: #a0aec0; margin: 6px 0 0; font-size: 13px; letter-spacing: 1px;">
                        Account Notification
                      </p>
                    </div>
                    <div style="padding: 40px;">
                      <p style="color: #4a5568; font-size: 16px; margin: 0 0 16px;">Hello,</p>
                      <p style="color: #4a5568; font-size: 15px; margin: 0; line-height: 1.7;">%s</p>
                    </div>
                    <div style="background-color: #f8f9fa; padding: 20px;
                                text-align: center; border-top: 1px solid #edf2f7;">
                      <p style="color: #cbd5e0; font-size: 12px; margin: 0;">
                        Copyright 2026 AI-Powered Resume Builder. All rights reserved.
                      </p>
                    </div>
                  </div>
                </div>
                """.formatted(messageText);
    }
}
