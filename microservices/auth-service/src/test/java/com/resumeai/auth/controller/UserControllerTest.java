package com.resumeai.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeai.auth.dtos.*;
import com.resumeai.auth.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private com.resumeai.auth.service.JwtService jwtService;

    @MockBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testWelcome() throws Exception {
        mockMvc.perform(get("/api/v1/auth/welcome"))
                .andExpect(status().isOk())
                .andExpect(content().string("welcome! It is working."));
    }

    @Test
    void testRegistrationRequest() throws Exception {
        RegisterRequest request = new RegisterRequest();
        when(userService.registerRequest(any(RegisterRequest.class))).thenReturn("OTP Sent");

        mockMvc.perform(post("/api/v1/auth/register-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("OTP Sent"));
    }

    @Test
    void testRegisterUsers() throws Exception {
        when(userService.registerUser("test@gmail.com", "123456")).thenReturn(new AuthResponse());

        mockMvc.perform(post("/api/v1/auth/register-user")
                        .param("email", "test@gmail.com")
                        .param("otp", "123456"))
                .andExpect(status().isOk());
    }

    @Test
    void testLogin() throws Exception {
        LoginRequest request = new LoginRequest();
        when(userService.loginUser(any(LoginRequest.class))).thenReturn(new AuthResponse());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void testRequestOtp() throws Exception {
        when(userService.initiateForgetPassword("test@gmail.com")).thenReturn("OTP Sent");

        mockMvc.perform(post("/api/v1/auth/forgot-password/request")
                        .param("email", "test@gmail.com"))
                .andExpect(status().isOk());
    }

    @Test
    void testVerifyOtp() throws Exception {
        when(userService.verifyOtp("test@gmail.com", "123456")).thenReturn("Verified");

        mockMvc.perform(post("/api/v1/auth/forgot-password/verify")
                        .param("email", "test@gmail.com")
                        .param("otp", "123456"))
                .andExpect(status().isOk());
    }

    @Test
    void testResetPassword() throws Exception {
        when(userService.resetPassword("test@gmail.com", "newpass")).thenReturn("Password Reset");

        mockMvc.perform(post("/api/v1/auth/forgot-password/reset")
                        .param("email", "test@gmail.com")
                        .param("newPassword", "newpass"))
                .andExpect(status().isOk());
    }

    @Test
    void testGetUserByEmail() throws Exception {
        when(userService.getUserByEmail("test@gmail.com")).thenReturn(new UserResponseDTO());

        mockMvc.perform(get("/api/v1/auth/test@gmail.com"))
                .andExpect(status().isOk());
    }

    @Test
    void testGetUserById() throws Exception {
        when(userService.getUserById(1L)).thenReturn(new UserResponseDTO());

        mockMvc.perform(get("/api/v1/auth/id/1"))
                .andExpect(status().isOk());
    }

    @Test
    void testUpdateProfile() throws Exception {
        UpdateProfileRequest request = new UpdateProfileRequest();
        when(userService.updateProfile(eq("test@gmail.com"), any(UpdateProfileRequest.class)))
                .thenReturn(new AuthResponse());

        mockMvc.perform(post("/api/v1/auth/update-profile/test@gmail.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void testVerifyEmailUpdate() throws Exception {
        when(userService.verifyEmailUpdate("test@gmail.com", "123456")).thenReturn(new AuthResponse());

        mockMvc.perform(post("/api/v1/auth/verify-email-update")
                        .param("currentEmail", "test@gmail.com")
                        .param("otp", "123456"))
                .andExpect(status().isOk());
    }

    @Test
    void testUpdateSubscription() throws Exception {
        doNothing().when(userService).updateSubscription("test@gmail.com", "PRO");

        mockMvc.perform(post("/api/v1/auth/update-subscription")
                        .param("email", "test@gmail.com")
                        .param("plan", "PRO"))
                .andExpect(status().isOk())
                .andExpect(content().string("Subscription updated to PRO"));
    }

    @Test
    void testRefreshToken() throws Exception {
        when(userService.refreshToken("test@gmail.com")).thenReturn(new AuthResponse());

        mockMvc.perform(post("/api/v1/auth/refresh-token")
                        .header("X-User-Email", "test@gmail.com"))
                .andExpect(status().isOk());
    }
}