package com.resumeai.aiservice.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ai_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiRequest {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "request_id")
	private UUID requestId;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "resume_id")
	private Long resumeId;

	@Enumerated(EnumType.STRING)
	@Column(name = "request_type", nullable = false)
	private RequestType requestType;

	@Lob
	@Column(name = "input_prompt", columnDefinition = "LONGTEXT")
	private String inputPrompt;

	@Lob
	@Column(name = "ai_response", columnDefinition = "LONGTEXT")
	private String aiResponse;

	@Column(name = "model")
	private String model;

	@Column(name = "tokens_used")
	private Integer tokensUsed;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false)
	private RequestStatus status;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "completed_at")
	private LocalDateTime completedAt;

	@Lob
	@Column(name = "error_message", columnDefinition = "LONGTEXT")
	private String errorMessage;

	public enum RequestType {
		SUMMARY, BULLETS, COVER_LETTER, IMPROVE, ATS, SKILLS, TAILOR, TRANSLATE
	}

	public enum RequestStatus {
		QUEUED, PROCESSING, COMPLETED, FAILED
	}
}

