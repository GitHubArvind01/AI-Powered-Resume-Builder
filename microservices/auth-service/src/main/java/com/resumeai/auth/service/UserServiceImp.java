package com.resumeai.auth.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.resumeai.auth.dtos.LoginRequest;
import com.resumeai.auth.dtos.RegisterRequest;
import com.resumeai.auth.dtos.AuthResponse;
import com.resumeai.auth.entity.User;
import com.resumeai.auth.repository.UserRepository;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class UserService {
	private static final SecureRandom random = new SecureRandom();
	
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final JavaMailSender mailSender;

	// register request
	public String registerRequest(RegisterRequest registerRequest) {

		// Step 1: check duplicate
		User existingUser = userRepository.findByEmail(registerRequest.getEmail()).orElse(null);
		if (existingUser != null) {
			throw new RuntimeException("User already registered with this email!");
		}

		// Step 2: Generate OTP
		String otp = generateOtp();

		// Step 3: Create user (NOT fully active yet)
		User user = new User();
		user.setUsername(registerRequest.getUsername());
		user.setEmail(registerRequest.getEmail());
		user.setPassword(registerRequest.getPassword()); // encode later after OTP verify
		user.setRole(registerRequest.getRole());

		user.setOtpCode(otp);
		user.setOtpExpiry(LocalDateTime.now().plusMinutes(5));

		userRepository.save(user);

		// Step 4: Send email
		sendOtpEmail(user.getEmail(), otp, "Registration OTP", "REGISTER");

		return "OTP sent to your email for verification.";
	}

	public AuthResponse registerUser(String email, String otp) {
		User userdb = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found!"));

		/*
		 * Here First we validate the OTP with our DB
		 */
		if (userdb.getOtpCode() == null || !userdb.getOtpCode().equals(otp)) {
			throw new RuntimeException("OTP Invalid! please try again.");
		}

		/*
		 * here - Validatation of OTP is expire or not
		 */
		if (userdb.getOtpCode() == null || userdb.getOtpExpiry().isBefore(LocalDateTime.now())) {
			throw new RuntimeException("OTP Expired! please try again.");
		}

		userdb.setPassword(passwordEncoder.encode(userdb.getPassword()));
		userdb.setOtpCode(null);
		userRepository.save(userdb);
		String token = jwtService.genrateToken(email);
		return new AuthResponse(token, "User Register success");
	}

	// login
	public AuthResponse loginUser(LoginRequest loginRequest) {
		User userdb = userRepository.findByEmail(loginRequest.getEmail())
				.orElseThrow(() -> new RuntimeException("User not found!"));

		if (!passwordEncoder.matches(loginRequest.getPassword(), userdb.getPassword())) {
			throw new RuntimeException("Invalid Password!");
		}
		String token = jwtService.genrateToken(loginRequest.getEmail());
		return new AuthResponse(token, "Login Success");
	}

	/*
	 * First We validate email- that exit in our database or not If email exist in
	 * our database then I will send 6-digit verification code to email otherwise
	 * stop
	 */
	public String initiateForgetPassword(String email) {
		User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found!"));

		// 1. Generate 6-digit code
		String otp = generateOtp();

		// 2. Save OTP and Expiry (e.g., 5 minutes from now) to DB
		user.setOtpCode(otp);
		user.setOtpExpiry(LocalDateTime.now().plusMinutes(5));
		userRepository.save(user);

		// 3. Here I will- Call to EmailService to send 'otp' to 'email' with subject,
		// and purpose
		sendOtpEmail(email, otp, "Forgot Password OTP", "FORGOT_PASSWORD");

		return "Verification code sent to your email.";
	}

	/*
	 * Once user get OTP then we verify that in our DB -
	 */

	public String verifyOtp(String email, String otp) {
		User userdb = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found!"));

		/*
		 * Here First we validate the OTP with our DB
		 */
		if (userdb.getOtpCode() == null || !userdb.getOtpCode().equals(otp)) {
			throw new RuntimeException("OTP Invalid! please try again.");
		}

		/*
		 * here - Validatation of OTP is expire or not
		 */
		if (userdb.getOtpCode() == null || !userdb.getOtpExpiry().isAfter(LocalDateTime.now())) {
			throw new RuntimeException("OTP Expired! please try again.");
		}
		return "OTP Verified. You may now reset your password.";
	}

	/*
	 * Finally we update the password
	 */
	public String resetPassword(String email, String newPassword) {
		User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found!"));

		// Ensure you encode the password before saving!
		user.setPassword(passwordEncoder.encode(newPassword));
		user.setOtpCode(null); // Clear OTP after use
		userRepository.save(user);

		return "Password updated successfully.";
	}

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
	
	/*
	 * This function will generate OTP
	 */
	private String generateOtp() {
	    int otp = 100000 + random.nextInt(900000);
	    return String.valueOf(otp);
	}
}