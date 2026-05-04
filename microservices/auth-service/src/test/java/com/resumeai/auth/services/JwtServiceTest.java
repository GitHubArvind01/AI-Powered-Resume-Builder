package com.resumeai.auth.services;

import com.resumeai.auth.entity.User;
import com.resumeai.auth.service.JwtService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private User testUser;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        // Set properties
        ReflectionTestUtils.setField(jwtService, "SECRET", "verylongsecretkeyrequiredbyhs256algorithmthatissufficientlylong");
        ReflectionTestUtils.setField(jwtService, "expirationTime", 3600000L); // 1 hour
        jwtService.init();

        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@gmail.com");
        testUser.setRole("USER");
        testUser.setSubscriptionPlan("FREE");
    }

    @Test
    void testTokenGenerationAndValidation() {
        String token = jwtService.generateToken(testUser);
        assertNotNull(token);

        String email = jwtService.extractEmail(token);
        assertEquals("test@gmail.com", email);

        boolean isValid = jwtService.validateToken(token, "test@gmail.com");
        assertTrue(isValid);
    }

    @Test
    void testExtractRole() {
        String token = jwtService.generateToken(testUser);
        String role = jwtService.extractRole(token);
        assertEquals("USER", role);
    }

    @Test
    void testExtractAllClaims() {
        String token = jwtService.generateToken(testUser);
        Claims claims = jwtService.extractAllClaims(token);

        assertNotNull(claims);
        assertEquals(1, claims.get("userId"));
        assertEquals("FREE", claims.get("subscriptionPlan"));
    }
}