package com.resumeai.aiservice.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiRequestDTO {

	private UUID requestId;

	@NotNull(message = "User ID is required")
	private Long userId;

	private Long resumeId;

	@NotNull(message = "Request type is required")
	private String requestType;

	@NotBlank(message = "Input prompt cannot be blank")
	private String inputPrompt;

	private String aiResponse;

	private String model;

	private Integer tokensUsed;

	private String status;

	private LocalDateTime createdAt;

	private LocalDateTime completedAt;

	private String errorMessage;
}

