package com.resumeai.auth.services;

import com.resumeai.auth.dtos.AuthResponse;
import com.resumeai.auth.entity.User;
import com.resumeai.auth.exception.UnauthorizedException;
import com.resumeai.auth.repository.UserRepository;
import com.resumeai.auth.service.CustomUserDetailsService;
import com.resumeai.auth.service.GoogleAuthService;
import com.resumeai.auth.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GoogleAuthServiceTest {

    @Mock
    private CustomUserDetailsService customUserDetailsService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RestTemplate restTemplate;
    @Mock
    private JwtService jwtService;

    @InjectMocks
    private GoogleAuthService googleAuthService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(googleAuthService, "clientId", "mock-client-id");
        ReflectionTestUtils.setField(googleAuthService, "clientSecret", "mock-secret");
        ReflectionTestUtils.setField(googleAuthService, "redirectUri", "mock-uri");
    }

    @Test
    void testHandleGoogleAuth_ExistingUser() {
        Map<String, Object> tokenResponseMap = new HashMap<>();
        tokenResponseMap.put("id_token", "mock-id-token");
        ResponseEntity<Map> tokenResponse = new ResponseEntity<>(tokenResponseMap, HttpStatus.OK);

        Map<String, Object> userInfoMap = new HashMap<>();
        userInfoMap.put("email", "test@gmail.com");
        ResponseEntity<Map> userInfoResponse = new ResponseEntity<>(userInfoMap, HttpStatus.OK);

        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Map.class))).thenReturn(tokenResponse);
        when(restTemplate.getForEntity(anyString(), eq(Map.class))).thenReturn(userInfoResponse);

        UserDetails userDetails = mock(UserDetails.class);
        when(customUserDetailsService.loadUserByEmail("test@gmail.com")).thenReturn(userDetails);

        User mockUser = new User();
        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(mockUser));
        when(jwtService.generateToken(mockUser)).thenReturn("mock-jwt-token");

        AuthResponse response = googleAuthService.handleGoogleAuth("mock-auth-code");

        assertNotNull(response);
        assertEquals("mock-jwt-token", response.getToken());
    }

    @Test
    void testHandleGoogleAuth_NewUser() {
        Map<String, Object> tokenResponseMap = new HashMap<>();
        tokenResponseMap.put("id_token", "mock-id-token");
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(tokenResponseMap, HttpStatus.OK));

        Map<String, Object> userInfoMap = new HashMap<>();
        userInfoMap.put("email", "newuser@gmail.com");
        when(restTemplate.getForEntity(anyString(), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(userInfoMap, HttpStatus.OK));

        // Simulate user not found
        when(customUserDetailsService.loadUserByEmail("newuser@gmail.com")).thenThrow(new RuntimeException("Not found"));
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPass");

        User savedUser = new User();
        when(userRepository.findByEmail("newuser@gmail.com")).thenReturn(Optional.of(savedUser));
        when(jwtService.generateToken(any(User.class))).thenReturn("mock-jwt-token");

        AuthResponse response = googleAuthService.handleGoogleAuth("mock-auth-code");

        verify(userRepository, times(1)).save(any(User.class));
        assertNotNull(response);
        assertEquals("mock-jwt-token", response.getToken());
    }

    @Test
    void testHandleGoogleAuth_ExceptionThrown() {
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new RuntimeException("API Down"));

        assertThrows(UnauthorizedException.class, () -> googleAuthService.handleGoogleAuth("mock-auth-code"));
    }
}