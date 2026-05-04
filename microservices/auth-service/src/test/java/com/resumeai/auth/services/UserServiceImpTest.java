package com.resumeai.auth.services;

import com.resumeai.auth.dtos.*;
import com.resumeai.auth.entity.User;
import com.resumeai.auth.exception.BadRequestException;
import com.resumeai.auth.exception.UnauthorizedException;
import com.resumeai.auth.messaging.EmailEventProducer;
import com.resumeai.auth.repository.UserRepository;
import com.resumeai.auth.service.JwtService;
import com.resumeai.auth.service.UserServiceImp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImpTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private EmailEventProducer emailEventProducer;

    @InjectMocks
    private UserServiceImp userService;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setEmail("test@gmail.com");
        mockUser.setPasswordHash("hashedPassword");
        mockUser.setFullName("Test User");
        mockUser.setActive(true);
    }

    @Test
    void testRegisterRequest_NewUser() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("newuser@gmail.com");
        request.setFullName("New User");
        request.setPassword("password");

        when(userRepository.findByEmail("newuser@gmail.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password")).thenReturn("hashedPass");

        String response = userService.registerRequest(request);

        assertEquals("OTP sent to your email for verification.", response);
        verify(userRepository, times(1)).save(any(User.class));
        verify(emailEventProducer, times(1)).publishEmailEvent(any(EmailEvent.class));
    }

    @Test
    void testRegisterRequest_ExistingActiveUserThrowsException() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@gmail.com");
        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(mockUser)); // mockUser is active

        assertThrows(RuntimeException.class, () -> userService.registerRequest(request));
    }

    @Test
    void testRegisterUser_Success() {
        mockUser.setActive(false);
        mockUser.setOtpCode("123456");
        mockUser.setOtpExpiry(LocalDateTime.now().plusMinutes(5));

        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(mockUser));
        when(jwtService.generateToken(mockUser)).thenReturn("jwt-token");

        AuthResponse response = userService.registerUser("test@gmail.com", "123456");

        assertNotNull(response);
        assertEquals("jwt-token", response.getToken());
        assertTrue(mockUser.isActive());
        assertNull(mockUser.getOtpCode());
    }

    @Test
    void testRegisterUser_ExpiredOtpThrowsException() {
        mockUser.setOtpCode("123456");
        mockUser.setOtpExpiry(LocalDateTime.now().minusMinutes(1)); // Expired
        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(mockUser));

        assertThrows(BadRequestException.class, () -> userService.registerUser("test@gmail.com", "123456"));
    }

    @Test
    void testRegisterUser_InvalidOtpThrowsException() {
        // Arrange: Set a specific OTP in the DB
        mockUser.setOtpCode("123456");
        mockUser.setOtpExpiry(LocalDateTime.now().plusMinutes(5));
        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(mockUser));

        // Act & Assert: Provide a WRONG OTP ("654321")
        BadRequestException exception = assertThrows(BadRequestException.class, () ->
                userService.registerUser("test@gmail.com", "654321")
        );

        // FIX: Changed "one." to "OTP." at the end of the string
        assertEquals("Invalid OTP. Please check the latest code from your email or request a new OTP.", exception.getMessage());
    }

    @Test
    void testLoginUser_Success() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@gmail.com");
        request.setPassword("password");

        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("password", "hashedPassword")).thenReturn(true);
        when(jwtService.generateToken(mockUser)).thenReturn("jwt-token");

        AuthResponse response = userService.loginUser(request);
        assertNotNull(response);
        assertEquals("jwt-token", response.getToken());
    }

    @Test
    void testLoginUser_BadPasswordThrowsException() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@gmail.com");
        request.setPassword("wrongpass");

        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("wrongpass", "hashedPassword")).thenReturn(false);

        assertThrows(RuntimeException.class, () -> userService.loginUser(request));
    }

    @Test
    void testInitiateForgetPassword() {
        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(mockUser));

        String response = userService.initiateForgetPassword("test@gmail.com");

        assertEquals("Verification code sent to your email.", response);
        verify(emailEventProducer, times(1)).publishEmailEvent(any(EmailEvent.class));
    }

    @Test
    void testResetPassword() {
        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.encode("newpassword")).thenReturn("newHashedPass");

        String response = userService.resetPassword("test@gmail.com", "newpassword");

        assertEquals("Password updated successfully.", response);
        assertEquals("newHashedPass", mockUser.getPasswordHash());
    }

    @Test
    void testUpdateProfile_EmailChange() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setEmail("newemail@gmail.com");
        request.setFullName("Updated Name");
        request.setPhone("1234567890");

        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(mockUser));
        when(userRepository.findByEmail("newemail@gmail.com")).thenReturn(Optional.empty()); // Email available

        UserResponseDTO response = userService.updateProfile("test@gmail.com", request);

        assertEquals("Updated Name", mockUser.getFullName());
        assertEquals("newemail@gmail.com", mockUser.getPendingEmail());
        verify(emailEventProducer, times(1)).publishEmailEvent(any(EmailEvent.class));
    }

    @Test
    void testVerifyEmailUpdate() {
        mockUser.setPendingEmail("newemail@gmail.com");
        mockUser.setOtpCode("123456");
        mockUser.setOtpExpiry(LocalDateTime.now().plusMinutes(5));

        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(mockUser));

        String response = userService.verifyEmailUpdate("test@gmail.com", "123456");

        assertEquals("Email updated successfully to newemail@gmail.com", response);
        assertEquals("newemail@gmail.com", mockUser.getEmail());
        assertNull(mockUser.getPendingEmail());
    }

    @Test
    void testRefreshToken() {
        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(mockUser));
        when(jwtService.generateToken(mockUser)).thenReturn("new-token");

        AuthResponse response = userService.refreshToken("test@gmail.com");
        assertEquals("new-token", response.getToken());
    }

    @Test
    void testGetCurrentUser_DeactivatedThrowsException() {
        mockUser.setActive(false);
        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(mockUser));

        assertThrows(UnauthorizedException.class, () -> userService.getCurrentUser("test@gmail.com"));
    }
}