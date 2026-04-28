package com.resumeai.auth;

import com.resumeai.auth.dtos.*;
import com.resumeai.auth.entity.User;
import com.resumeai.auth.exception.BadRequestException;
import com.resumeai.auth.messaging.EmailEventProducer;
import com.resumeai.auth.repository.UserRepository;
import com.resumeai.auth.service.JwtService;
import com.resumeai.auth.service.UserServiceImp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceHighCoverageTest {

	@Mock private UserRepository userRepository;
	@Mock private PasswordEncoder passwordEncoder;
	@Mock private JwtService jwtService;
	@Mock private EmailEventProducer emailEventProducer;

	@InjectMocks
	private UserServiceImp userService;

	private User user;

	@BeforeEach
	void setup() {
		user = new User();
		user.setEmail("kumar@example.com");
		user.setPasswordHash("encoded-password");
		user.setActive(true);
	}

	// ================= REGISTER =================

	@Test
	void registerRequest_newUser_success() {
		when(userRepository.findByEmail("kumar@example.com")).thenReturn(Optional.empty());
		when(passwordEncoder.encode(any())).thenReturn("encoded");

		RegisterRequest request = new RegisterRequest("Kumar", "kumar@example.com", "pass", "123");

		String result = userService.registerRequest(request);

		assertEquals("OTP sent to your email for verification.", result);
		verify(userRepository).save(any(User.class));
		verify(emailEventProducer).publishEmailEvent(any());
	}

	@Test
	void registerRequest_activeUser_exists_throw() {
		user.setActive(true);
		when(userRepository.findByEmail("kumar@example.com")).thenReturn(Optional.of(user));

		RegisterRequest request = new RegisterRequest("Kumar", "kumar@example.com", "pass", "123");

		assertThrows(RuntimeException.class,
				() -> userService.registerRequest(request));
	}

	@Test
	void registerRequest_inactiveUser_resendOtp() {
		user.setActive(false);
		when(userRepository.findByEmail("kumar@example.com")).thenReturn(Optional.of(user));

		RegisterRequest request = new RegisterRequest("Kumar", "kumar@example.com", "pass", "123");

		userService.registerRequest(request);

		verify(userRepository).save(any());
		verify(emailEventProducer).publishEmailEvent(any());
	}

	// ================= REGISTER VERIFY =================

	@Test
	void registerUser_validOtp_success() {
		user.setActive(false);
		user.setOtpCode("123456");
		user.setOtpExpiry(LocalDateTime.now().plusMinutes(5));

		when(userRepository.findByEmail("kumar@example.com")).thenReturn(Optional.of(user));
		when(jwtService.generateToken(user)).thenReturn("token");

		AuthResponse response = userService.registerUser("kumar@example.com", "123456");

		assertEquals("token", response.getToken());
		assertTrue(user.isActive());
		verify(userRepository).save(user);
	}

	@Test
	void registerUser_wrongOtp_throw() {
		user.setOtpCode("123456");
		user.setOtpExpiry(LocalDateTime.now().plusMinutes(5));

		when(userRepository.findByEmail("kumar@example.com")).thenReturn(Optional.of(user));

		assertThrows(BadRequestException.class,
				() -> userService.registerUser("kumar@example.com", "000000"));
	}

	@Test
	void registerUser_expiredOtp_throw() {
		user.setOtpCode("123456");
		user.setOtpExpiry(LocalDateTime.now().minusMinutes(1));

		when(userRepository.findByEmail("kumar@example.com")).thenReturn(Optional.of(user));

		assertThrows(BadRequestException.class,
				() -> userService.registerUser("kumar@example.com", "123456"));
	}

	@Test
	void registerUser_userNotFound_throw() {
		when(userRepository.findByEmail("kumar@example.com")).thenReturn(Optional.empty());

		assertThrows(RuntimeException.class,
				() -> userService.registerUser("kumar@example.com", "123456"));
	}

	@Test
	void registerUser_blankOtp_throw() {
		assertThrows(Exception.class,
				() -> userService.registerUser("kumar@example.com", " "));
	}

	// ================= LOGIN =================

	@Test
	void login_success() {
		when(userRepository.findByEmail("kumar@example.com")).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("pass", "encoded-password")).thenReturn(true);
		when(jwtService.generateToken(user)).thenReturn("token");

		LoginRequest request = new LoginRequest("kumar@example.com", "pass");

		AuthResponse res = userService.loginUser(request);

		assertEquals("token", res.getToken());
	}

	@Test
	void login_userNotFound_throw() {
		when(userRepository.findByEmail(any())).thenReturn(Optional.empty());

		LoginRequest request = new LoginRequest("x@y.com", "pass");

		assertThrows(RuntimeException.class,
				() -> userService.loginUser(request));
	}

	@Test
	void login_wrongPassword_throw() {
		when(userRepository.findByEmail("kumar@example.com")).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("wrong", "encoded-password")).thenReturn(false);

		LoginRequest request = new LoginRequest("kumar@example.com", "wrong");

		assertThrows(RuntimeException.class,
				() -> userService.loginUser(request));
	}

	@Test
	void login_inactiveUser_throw() {
		user.setActive(false);

		when(userRepository.findByEmail("kumar@example.com")).thenReturn(Optional.of(user));
		when(passwordEncoder.matches(any(), any())).thenReturn(true);

		LoginRequest request = new LoginRequest("kumar@example.com", "pass");

		assertThrows(RuntimeException.class,
				() -> userService.loginUser(request));
	}

	// ================= RESET PASSWORD =================

	@Test
	void resetPassword_success() {
		user.setOtpCode("123456");

		when(userRepository.findByEmail("kumar@example.com")).thenReturn(Optional.of(user));
		when(passwordEncoder.encode("new")).thenReturn("encoded-new");

		String res = userService.resetPassword("kumar@example.com", "new");

		assertEquals("Password updated successfully.", res);
		assertNull(user.getOtpCode());
		verify(userRepository).save(user);
	}

	@Test
	void resetPassword_userNotFound_throw() {
		when(userRepository.findByEmail(any())).thenReturn(Optional.empty());

		assertThrows(RuntimeException.class,
				() -> userService.resetPassword("x@y.com", "pass"));
	}
}