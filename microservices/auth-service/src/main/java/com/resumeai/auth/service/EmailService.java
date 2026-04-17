package com.resumeai.auth.service;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class EmailService {
	
	private final JavaMailSender mailSender;
	
	/*
	 * This sendOtpEmail function work for all - i will add message those method
	 * call them they send message which purpose they are calling and I will add in
	 * that, what it take - Subject, Purpose message like- registeration new user,
	 * forget password like that
	 */
	public void sendOtpEmail(String toEmail, String otp, String subject, String purpose) {
		try {
			MimeMessage message = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

			helper.setTo(toEmail);
			helper.setSubject("Secure Verification Code");

			String messageText = switch (purpose) {
				case "REGISTER" -> "Use this OTP to complete your registration. This code is valid for 5 minutes.";
				case "FORGOT_PASSWORD" -> "Use the following code to reset your password. This code is valid for 5 minutes.";
				default -> "Use this OTP for verification.";
			};

			// Email Template
			String htmlContent = """
					<div style="font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f4f7f6; padding: 40px; border-radius: 10px;">
					    <div style="max-width: 500px; margin: auto; background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.1);">
					        <div style="background-color: #2c3e50; padding: 20px; text-align: center;">
					            <h1 style="color: #ffffff; margin: 0; font-size: 24px; letter-spacing: 2px;">SECURE AUTH</h1>
					        </div>
					        <div style="padding: 40px; text-align: center;">
					            <p style="color: #555; font-size: 16px;">Hello,</p>
					            <p style="color: #555; font-size: 16px;"> <p>%s</p> </p>
					            <div style="margin: 30px 0;">
					                <span style="font-size: 36px; font-weight: bold; color: #2828ff; letter-spacing: 8px; padding: 10px 20px; border: 2px dashed #2828ff; border-radius: 5px; background-color: #f0f4ff;">
					                    %s
					                </span>
					            </div>
					            <p style="color: #999; font-size: 13px;">If you did not request this, please ignore this email.</p>
					        </div>
					        <div style="background-color: #f9f9f9; padding: 20px; text-align: center; border-top: 1px solid #eee;">
					            <p style="color: #bbb; font-size: 12px; margin: 0;">© 2026 Secure Auth Systems Inc.</p>
					        </div>
					    </div>
					</div>
					"""
					.formatted(messageText, otp);

			helper.setText(htmlContent, true); // 'true' enables HTML
			mailSender.send(message);

		} catch (MessagingException e) {
			throw new RuntimeException("Failed to send email", e);
		}
	}
}