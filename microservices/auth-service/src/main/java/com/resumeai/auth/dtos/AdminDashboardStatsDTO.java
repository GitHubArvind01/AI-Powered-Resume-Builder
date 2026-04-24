package com.resumeai.auth.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardStatsDTO {
    private long totalUsers;
    private long activeUsers;
    private long inactiveUsers;
    private long premiumUsers;
    private long freeUsers;
    private long adminUsers;
    private long regularUsers;
}
