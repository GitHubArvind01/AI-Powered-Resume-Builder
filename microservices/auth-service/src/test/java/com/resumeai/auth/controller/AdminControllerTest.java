package com.resumeai.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeai.auth.dtos.*;
import com.resumeai.auth.service.AdminService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private com.resumeai.auth.service.JwtService jwtService;

    @MockBean
    private AdminService adminService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testGetDashboardStats() throws Exception {
        when(adminService.getDashboardStats()).thenReturn(new AdminDashboardStatsDTO());

        mockMvc.perform(get("/api/v1/admin/stats"))
                .andExpect(status().isOk());
        verify(adminService, times(1)).getDashboardStats();
    }

    @Test
    void testGetUsers() throws Exception {
        when(adminService.getAllUsers()).thenReturn(Arrays.asList(new AdminUserSummaryDTO()));

        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isOk());
        verify(adminService, times(1)).getAllUsers();
    }

    @Test
    void testGetUser() throws Exception {
        when(adminService.getUserById(1L)).thenReturn(new AdminUserDetailsDTO());

        mockMvc.perform(get("/api/v1/admin/users/1"))
                .andExpect(status().isOk());
        verify(adminService, times(1)).getUserById(1L);
    }

    @Test
    void testUpdateUser() throws Exception {
        AdminUpdateUserRequest request = new AdminUpdateUserRequest();
        // Add valid data to pass the @Valid checks!
        request.setFullName("Test User");
        request.setEmail("test@gmail.com");
        request.setRole("USER");
        request.setSubscriptionPlan("FREE");
        request.setActive(true);

        when(adminService.updateUser(eq(1L), any(AdminUpdateUserRequest.class))).thenReturn(new AdminUserDetailsDTO());

        mockMvc.perform(put("/api/v1/admin/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(adminService, times(1)).updateUser(eq(1L), any(AdminUpdateUserRequest.class));
    }

    @Test
    void testDeleteUser() throws Exception {
        doNothing().when(adminService).deleteUser(1L);

        mockMvc.perform(delete("/api/v1/admin/users/1"))
                .andExpect(status().isNoContent());
        verify(adminService, times(1)).deleteUser(1L);
    }

    @Test
    void testDeactivateUser() throws Exception {
        when(adminService.deactivateUser(1L)).thenReturn(new AdminUserDetailsDTO());

        mockMvc.perform(patch("/api/v1/admin/users/1/deactivate"))
                .andExpect(status().isOk());
        verify(adminService, times(1)).deactivateUser(1L);
    }

    @Test
    void testActivateUser() throws Exception {
        when(adminService.activateUser(1L)).thenReturn(new AdminUserDetailsDTO());

        mockMvc.perform(patch("/api/v1/admin/users/1/activate"))
                .andExpect(status().isOk());
        verify(adminService, times(1)).activateUser(1L);
    }
}