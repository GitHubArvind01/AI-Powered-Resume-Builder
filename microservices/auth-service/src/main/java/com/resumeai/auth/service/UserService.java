package com.resumeai.auth.service;

import com.resumeai.auth.dtos.AuthResponse;
import com.resumeai.auth.dtos.LoginRequest;
import com.resumeai.auth.dtos.RegisterRequest;
import com.resumeai.auth.dtos.UpdateProfileRequest;
import com.resumeai.auth.dtos.UserResponseDTO;

public interface UserService {

    /*
     * AUTH - REGISTRATION
     */
    String registerRequest(RegisterRequest registerRequest);

    AuthResponse registerUser(String email, String otp);

    /*
     * AUTH - LOGIN
     */
    AuthResponse loginUser(LoginRequest loginRequest);

    /*
     * FORGOT PASSWORD FLOW
     */
    String initiateForgetPassword(String email);

    String verifyOtp(String email, String otp);

    String resetPassword(String email, String newPassword);
    
    /*
     * Update User
     */
    void updateSubscription(String email, String plan);
    
    UserResponseDTO updateProfile(String email, UpdateProfileRequest updateUser);
    
    public String verifyEmailUpdate(String currentEmail, String otp);
    
    public UserResponseDTO getUserByEmail(String email);

    public UserResponseDTO getUserById(Long id);
}