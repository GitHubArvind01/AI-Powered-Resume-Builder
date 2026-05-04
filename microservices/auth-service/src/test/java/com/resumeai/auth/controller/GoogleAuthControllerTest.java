package com.resumeai.auth.controller;

import com.resumeai.auth.dtos.AuthResponse;
import com.resumeai.auth.service.GoogleAuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GoogleAuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class GoogleAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private com.resumeai.auth.service.JwtService jwtService;

    @MockBean
    private GoogleAuthService googleAuthService;

    @Test
    void testHandleGoogleCallback() throws Exception {
        String code = "test-auth-code";
        when(googleAuthService.handleGoogleAuth(code)).thenReturn(new AuthResponse());

        mockMvc.perform(get("/api/v1/auth/callback")
                        .param("code", code))
                .andExpect(status().isOk());

        verify(googleAuthService, times(1)).handleGoogleAuth(code);
    }
}