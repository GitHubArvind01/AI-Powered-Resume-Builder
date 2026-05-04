package com.resumeai.auth.services;

import com.resumeai.auth.entity.User;
import com.resumeai.auth.repository.UserRepository;
import com.resumeai.auth.service.CustomUserDetailsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void testLoadUserByEmail_Success() {
        User user = new User();
        user.setEmail("test@gmail.com");
        user.setFullName("Test User");
        user.setPasswordHash("hashedPass");
        user.setRole("USER");

        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(user));

        UserDetails result = customUserDetailsService.loadUserByEmail("test@gmail.com");

        assertNotNull(result);
        assertEquals("Test User", result.getUsername());
        assertEquals("hashedPass", result.getPassword());
    }

    @Test
    void testLoadUserByEmail_NotFound() {
        when(userRepository.findByEmail("notfound@gmail.com")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> customUserDetailsService.loadUserByEmail("notfound@gmail.com"));
    }
}