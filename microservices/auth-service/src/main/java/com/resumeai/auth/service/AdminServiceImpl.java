package com.resumeai.auth.service;

import java.util.Comparator;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.resumeai.auth.client.AdminResumeClient;
import com.resumeai.auth.dtos.AdminDashboardStatsDTO;
import com.resumeai.auth.dtos.AdminResumeDTO;
import com.resumeai.auth.dtos.AdminUpdateUserRequest;
import com.resumeai.auth.dtos.AdminUserDetailsDTO;
import com.resumeai.auth.dtos.AdminUserSummaryDTO;
import com.resumeai.auth.dtos.EmailEvent;
import com.resumeai.auth.entity.User;
import com.resumeai.auth.exception.BadRequestException;
import com.resumeai.auth.exception.ForbiddenException;
import com.resumeai.auth.exception.ResourceNotFoundException;
import com.resumeai.auth.messaging.EmailEventProducer;
import com.resumeai.auth.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final AdminResumeClient adminResumeClient;
    private final EmailEventProducer emailEventProducer;

    @Override
    @Transactional(readOnly = true)
    public List<AdminUserSummaryDTO> getAllUsers() {
        assertAdminAccess();

        return userRepository.findAll().stream()
                .map(user -> {
                    List<AdminResumeDTO> resumes = adminResumeClient.getResumesByUserId(user.getId());
                    return new AdminUserSummaryDTO(
                            user.getId(),
                            user.getFullName(),
                            user.getEmail(),
                            user.getRole(),
                            user.getSubscriptionPlan(),
                            user.isActive(),
                            user.getCreatedAt(),
                            resumes.size()
                    );
                })
                .sorted(Comparator.comparing(AdminUserSummaryDTO::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AdminUserDetailsDTO getUserById(Long id) {
        assertAdminAccess();
        User user = getExistingUser(id);
        return buildDetails(user);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminDashboardStatsDTO getDashboardStats() {
        assertAdminAccess();

        List<User> users = userRepository.findAll();
        long totalUsers = users.size();
        long activeUsers = users.stream().filter(User::isActive).count();
        long premiumUsers = users.stream()
                .filter(user -> {
                    String plan = user.getSubscriptionPlan() != null ? user.getSubscriptionPlan().toUpperCase() : "FREE";
                    return !"FREE".equals(plan);
                })
                .count();
        long adminUsers = users.stream().filter(user -> "ADMIN".equalsIgnoreCase(user.getRole())).count();

        return new AdminDashboardStatsDTO(
                totalUsers,
                activeUsers,
                totalUsers - activeUsers,
                premiumUsers,
                totalUsers - premiumUsers,
                adminUsers,
                totalUsers - adminUsers
        );
    }

    @Override
    @Transactional
    public AdminUserDetailsDTO updateUser(Long id, AdminUpdateUserRequest request) {
        assertAdminAccess();
        User user = getExistingUser(id);

        String normalizedEmail = request.getEmail().trim().toLowerCase();
        if (!user.getEmail().equalsIgnoreCase(normalizedEmail) && userRepository.existsByEmail(normalizedEmail)) {
            throw new BadRequestException("Email already exists for another user.");
        }

        user.setFullName(request.getFullName().trim());
        user.setEmail(normalizedEmail);
        user.setPhone(request.getPhone());
        user.setRole(request.getRole().trim().toUpperCase());
        user.setSubscriptionPlan(request.getSubscriptionPlan().trim().toUpperCase());
        user.setActive(Boolean.TRUE.equals(request.getActive()));

        return buildDetails(userRepository.save(user));
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        assertAdminAccess();
        User user = getExistingUser(id);
        preventSelfAction(user);

        publishUserActionEmail(
                user.getEmail(),
                "Your account has been permanently deleted from our platform.",
                "Account Deleted",
                "USER_DELETED"
        );

        userRepository.delete(user);
    }

    @Override
    @Transactional
    public AdminUserDetailsDTO deactivateUser(Long id) {
        assertAdminAccess();
        User user = getExistingUser(id);
        preventSelfAction(user);

        if (!user.isActive()) {
            throw new BadRequestException("User is already deactivated.");
        }

        user.setActive(false);
        User savedUser = userRepository.save(user);

        publishUserActionEmail(
                savedUser.getEmail(),
                "Your account has been deactivated. Contact support if needed.",
                "Account Deactivated",
                "USER_DEACTIVATED"
        );

        return buildDetails(savedUser);
    }

    @Override
    @Transactional
    public AdminUserDetailsDTO activateUser(Long id) {
        assertAdminAccess();
        User user = getExistingUser(id);

        if (user.isActive()) {
            throw new BadRequestException("User is already active.");
        }

        user.setActive(true);
        return buildDetails(userRepository.save(user));
    }

    private AdminUserDetailsDTO buildDetails(User user) {
        List<AdminResumeDTO> resumes = adminResumeClient.getResumesByUserId(user.getId());
        return new AdminUserDetailsDTO(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                user.getSubscriptionPlan(),
                user.isActive(),
                user.getCreatedAt(),
                resumes.size(),
                resumes
        );
    }

    private User getExistingUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    private void assertAdminAccess() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getAuthorities().stream()
                .noneMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()))) {
            throw new ForbiddenException("Only admins can perform this action.");
        }
    }

    private void preventSelfAction(User targetUser) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && targetUser.getEmail().equalsIgnoreCase(String.valueOf(authentication.getPrincipal()))) {
            throw new BadRequestException("Admin cannot deactivate or delete their own account.");
        }
    }

    private void publishUserActionEmail(String to, String message, String subject, String purpose) {
        emailEventProducer.publishEmailEvent(EmailEvent.builder()
                .to(to)
                .subject(subject)
                .purpose(purpose)
                .message(message)
                .build());
    }
}
