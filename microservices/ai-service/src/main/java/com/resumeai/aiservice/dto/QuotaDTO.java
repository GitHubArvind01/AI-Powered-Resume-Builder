package com.resumeai.aiservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuotaDTO {

	private Long userId;
	private Integer totalMonthlyQuota;
	private Integer usedQuota;
	private Integer remainingQuota;
	private String tierType; // FREE, PREMIUM, ENTERPRISE
}

