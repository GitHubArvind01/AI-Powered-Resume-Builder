package com.resumeai.auth.service;

import java.util.List;

import com.resumeai.auth.dtos.AdminDashboardStatsDTO;
import com.resumeai.auth.dtos.AdminUpdateUserRequest;
import com.resumeai.auth.dtos.AdminUserDetailsDTO;
import com.resumeai.auth.dtos.AdminUserSummaryDTO;

public interface AdminService {

    List<AdminUserSummaryDTO> getAllUsers();

    AdminUserDetailsDTO getUserById(Long id);

    AdminDashboardStatsDTO getDashboardStats();

    AdminUserDetailsDTO updateUser(Long id, AdminUpdateUserRequest request);

    void deleteUser(Long id);

    AdminUserDetailsDTO deactivateUser(Long id);

    AdminUserDetailsDTO activateUser(Long id);
}
