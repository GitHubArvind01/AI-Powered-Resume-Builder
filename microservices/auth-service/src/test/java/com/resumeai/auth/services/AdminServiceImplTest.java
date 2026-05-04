package com.resumeai.auth.services;

import com.resumeai.auth.client.AdminResumeClient;
import com.resumeai.auth.dtos.*;
import com.resumeai.auth.entity.User;
import com.resumeai.auth.exception.BadRequestException;
import com.resumeai.auth.exception.ResourceNotFoundException;
import com.resumeai.auth.messaging.EmailEventProducer;
import com.resumeai.auth.repository.UserRepository;
import com.resumeai.auth.service.AdminServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private AdminResumeClient adminResumeClient;
    @Mock
    private EmailEventProducer emailEventProducer;

    @InjectMocks
    private AdminServiceImpl adminService;

    private User mockUser;

    @BeforeEach
    void setUp() {
        // Setup Security Context for Admin
        Authentication auth = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);

        GrantedAuthority authority = () -> "ROLE_ADMIN";

        // ADD 'lenient().' TO THESE TWO LINES:
        lenient().doReturn(Collections.singletonList(authority)).when(auth).getAuthorities();
        lenient().when(auth.getPrincipal()).thenReturn("admin@gmail.com");

        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setEmail("user@gmail.com");
        mockUser.setFullName("Test User");
        mockUser.setRole("USER");
        mockUser.setSubscriptionPlan("FREE");
        mockUser.setActive(true);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testGetAllUsers() {
        when(userRepository.findAll()).thenReturn(List.of(mockUser));
        when(adminResumeClient.getResumesByUserId(1L)).thenReturn(Collections.emptyList());

        List<AdminUserSummaryDTO> result = adminService.getAllUsers();
        assertEquals(1, result.size());
        verify(userRepository, times(1)).findAll();
    }

    @Test
    void testGetUserById() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(adminResumeClient.getResumesByUserId(1L)).thenReturn(Collections.emptyList());

        AdminUserDetailsDTO result = adminService.getUserById(1L);
        assertNotNull(result);
        assertEquals("user@gmail.com", result.getEmail());
    }

    @Test
    void testGetUserById_NotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> adminService.getUserById(1L));
    }

    @Test
    void testGetDashboardStats() {
        when(userRepository.findAll()).thenReturn(List.of(mockUser));

        AdminDashboardStatsDTO stats = adminService.getDashboardStats();
        assertEquals(1, stats.getTotalUsers());
        assertEquals(1, stats.getActiveUsers());
        assertEquals(0, stats.getPremiumUsers());
    }

    @Test
    void testUpdateUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        AdminUpdateUserRequest request = new AdminUpdateUserRequest();
        request.setEmail("updated@gmail.com");
        request.setFullName("Updated Name");
        request.setRole("USER");
        request.setSubscriptionPlan("PRO");
        request.setActive(true);

        AdminUserDetailsDTO result = adminService.updateUser(1L, request);
        assertNotNull(result);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void testDeleteUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        doNothing().when(emailEventProducer).publishEmailEvent(any(EmailEvent.class));
        doNothing().when(userRepository).delete(mockUser);

        adminService.deleteUser(1L);
        verify(userRepository).delete(mockUser);
    }

    @Test
    void testDeleteUser_SelfActionThrowsException() {
        mockUser.setEmail("admin@gmail.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));

        assertThrows(BadRequestException.class, () -> adminService.deleteUser(1L));
    }

    @Test
    void testDeactivateUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        adminService.deactivateUser(1L);
        assertFalse(mockUser.isActive());
        verify(emailEventProducer).publishEmailEvent(any(EmailEvent.class));
    }

    @Test
    void testActivateUser() {
        mockUser.setActive(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        adminService.activateUser(1L);
        assertTrue(mockUser.isActive());
    }
}