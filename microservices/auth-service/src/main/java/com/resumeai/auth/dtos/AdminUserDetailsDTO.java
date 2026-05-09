package com.resumeai.auth.dtos;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserDetailsDTO {
    private Long id;
    private String fullName;
    private String email;
    private String phone;
    private String role;
    private String subscriptionPlan;
    private boolean active;
    private LocalDateTime createdAt;
    private Integer resumeCount;
    private List<AdminResumeDTO> resumes;
    private Boolean premiumActive;
    private String subscriptionStatus;
    private LocalDateTime subscriptionStartDate;
    private LocalDateTime subscriptionEndDate;
    private String paymentStatus;
}
