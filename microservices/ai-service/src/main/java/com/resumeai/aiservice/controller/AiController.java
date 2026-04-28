package com.resumeai.aiservice.controller;

import java.util.List;
import java.util.Map;

import org.apache.tika.Tika;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.resumeai.aiservice.dto.AiRequestDTO;
import com.resumeai.aiservice.dto.AtsReportDTO;
import com.resumeai.aiservice.dto.QuotaDTO;
import com.resumeai.aiservice.service.AiService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "AI Content Service", description = "APIs for AI-powered resume content generation")
public class AiController {

	private final AiService aiService;
	private final Tika tika = new Tika();

	/**
	 * Welcome endpoint to check if service is running
	 */
	@GetMapping("/welcome")
	@Operation(summary = "Health Check", description = "Check if AI Service is running")
	public ResponseEntity<String> welcome() {
		return ResponseEntity.ok("AI Service is running!");
	}


	@PostMapping("/generate-summary")
	@Operation(summary = "Generate Professional Summary",
			description = "Generates a professional summary from resume content")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Summary generated successfully",
					content = @Content(schema = @Schema(implementation = AiRequestDTO.class))),
			@ApiResponse(responseCode = "402", description = "Monthly quota exceeded"),
			@ApiResponse(responseCode = "500", description = "AI provider error")
	})
	public ResponseEntity<AiRequestDTO> generateSummary(@Valid @RequestBody SummaryRequest request)
			throws Exception {
		log.info("Generating summary for user: {}, resumeId: {}", request.getUserId(), request.getResumeId());
		AiRequestDTO response = aiService.generateSummary(request.getUserId(), request.getResumeId(),
				request.getResumeContent());
		return ResponseEntity.status(HttpStatus.OK).body(response);
	}

	@PostMapping("/improve")
	@Operation(summary = "Improve resume content",
			description = "Client-friendly endpoint used by the Angular editor to enhance resume text.")
	public ResponseEntity<ImproveContentResponse> improveContent(@Valid @RequestBody ImproveContentRequest request)
			throws Exception {
		log.info("Improving content for user: {}, resumeId: {}, type: {}", request.getUserId(), request.getResumeId(),
				request.getType());

		AiRequestDTO response = switch (request.getType()) {
			case "summary" -> aiService.generateSummary(request.getUserId(), request.getResumeId(), request.getText());
			case "bullets" -> aiService.generateBullets(request.getUserId(), request.getResumeId(), request.getText());
			case "skills" -> aiService.extractSkills(request.getUserId(), request.getResumeId(), request.getText());
			default -> aiService.improveResume(request.getUserId(), request.getResumeId(), request.getText());
		};

		return ResponseEntity.ok(new ImproveContentResponse(
				request.getText(),
				response.getAiResponse(),
				List.of("Improved with " + response.getModel()),
				0.95));
	}

	@GetMapping("/usage/{userId}")
	@Operation(summary = "Get AI usage summary", description = "Returns used and remaining AI quota for the current month.")
	public ResponseEntity<Map<String, Integer>> getUsage(@PathVariable Long userId) {
		QuotaDTO quota = aiService.getQuotaInfo(userId);
		return ResponseEntity.ok(Map.of(
				"usage", quota.getUsedQuota(),
				"remaining", quota.getRemainingQuota(),
				"total", quota.getTotalMonthlyQuota()));
	}

	@PostMapping("/generate-bullets")
	@Operation(summary = "Generate Achievement Bullets",
			description = "Extracts and optimizes achievement bullets from resume")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Bullets generated successfully",
					content = @Content(schema = @Schema(implementation = AiRequestDTO.class))),
			@ApiResponse(responseCode = "402", description = "Monthly quota exceeded"),
			@ApiResponse(responseCode = "500", description = "AI provider error")
	})
	public ResponseEntity<AiRequestDTO> generateBullets(@Valid @RequestBody BulletsRequest request)
			throws Exception {
		log.info("Generating bullets for user: {}, resumeId: {}", request.getUserId(), request.getResumeId());
		AiRequestDTO response = aiService.generateBullets(request.getUserId(), request.getResumeId(),
				request.getResumeContent());
		return ResponseEntity.status(HttpStatus.OK).body(response);
	}

	@PostMapping("/generate-cover-letter")
	@Operation(summary = "Generate Cover Letter",
			description = "Generates a tailored cover letter based on resume and job description")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Cover letter generated successfully",
					content = @Content(schema = @Schema(implementation = AiRequestDTO.class))),
			@ApiResponse(responseCode = "402", description = "Monthly quota exceeded"),
			@ApiResponse(responseCode = "500", description = "AI provider error")
	})
	public ResponseEntity<AiRequestDTO> generateCoverLetter(@Valid @RequestBody CoverLetterRequest request)
			throws Exception {
		log.info("Generating cover letter for user: {}, resumeId: {}", request.getUserId(), request.getResumeId());
		AiRequestDTO response = aiService.generateCoverLetter(request.getUserId(), request.getResumeId(),
				request.getResumeContent(), request.getJobDescription());
		return ResponseEntity.status(HttpStatus.OK).body(response);
	}

	@PostMapping("/check-ats")
	@Operation(summary = "Check ATS Compatibility",
			description = "Analyzes resume ATS compatibility with keyword matching and semantic similarity")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "ATS analysis completed",
					content = @Content(schema = @Schema(implementation = AtsReportDTO.class))),
			@ApiResponse(responseCode = "402", description = "Monthly quota exceeded"),
			@ApiResponse(responseCode = "500", description = "AI provider error")
	})
	public ResponseEntity<AtsReportDTO> checkAtsCompatibility(@Valid @RequestBody AtsRequest request)
			throws Exception {
		log.info("Checking ATS compatibility for user: {}, resumeId: {}", request.getUserId(), request.getResumeId());
		AtsReportDTO response = aiService.checkAtsCompatibility(request.getUserId(), request.getResumeId(),
				request.getResumeContent(), request.getJobDescription());
		return ResponseEntity.status(HttpStatus.OK).body(response);
	}

	@PostMapping("/check-ats/upload")
	@Operation(summary = "Check ATS Compatibility from uploaded resume",
			description = "Extracts text from an uploaded resume file and runs the same ATS analysis flow.")
	public ResponseEntity<AtsReportDTO> checkAtsCompatibilityFromUpload(
			@RequestHeader("X-User-Id") Long userId,
			@RequestParam("file") MultipartFile file,
			@RequestParam(value = "jobDescription", required = false, defaultValue = "") String jobDescription)
			throws Exception {
		if (file.isEmpty()) {
			throw new IllegalArgumentException("Please choose a resume file before starting ATS analysis.");
		}

		String resumeContent = tika.parseToString(file.getInputStream());
		if (resumeContent == null || resumeContent.trim().isEmpty()) {
			throw new IllegalArgumentException("We couldn't extract readable text from this file. Please try another file or paste the resume content.");
		}

		log.info("Checking ATS compatibility for uploaded file: {}, user: {}", file.getOriginalFilename(), userId);
		AtsReportDTO response = aiService.checkAtsCompatibility(userId, null, resumeContent, jobDescription);
		return ResponseEntity.status(HttpStatus.OK).body(response);
	}

	@PostMapping("/tailor-resume")
	@Operation(summary = "Tailor Resume for Job",
			description = "Holistic revision of resume JSON for a specific job description")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Resume tailored successfully",
					content = @Content(schema = @Schema(implementation = AiRequestDTO.class))),
			@ApiResponse(responseCode = "402", description = "Monthly quota exceeded"),
			@ApiResponse(responseCode = "500", description = "AI provider error")
	})
	public ResponseEntity<AiRequestDTO> tailorResume(@Valid @RequestBody TailorRequest request)
			throws Exception {
		log.info("Tailoring resume for user: {}, resumeId: {}", request.getUserId(), request.getResumeId());
		AiRequestDTO response = aiService.tailorResumeForJob(request.getUserId(), request.getResumeId(),
				request.getResumeJson(), request.getJobDescription());
		return ResponseEntity.status(HttpStatus.OK).body(response);
	}

	@GetMapping("/history/{userId}")
	@Operation(summary = "Get Request History",
			description = "Fetches AI request history for a specific user")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Request history retrieved",
					content = @Content(schema = @Schema(implementation = AiRequestDTO.class))),
			@ApiResponse(responseCode = "404", description = "User or history not found")
	})
	public ResponseEntity<List<AiRequestDTO>> getHistory(@PathVariable Long userId) {
		log.info("Fetching request history for user: {}", userId);
		List<AiRequestDTO> history = aiService.getRequestHistory(userId);
		return ResponseEntity.status(HttpStatus.OK).body(history);
	}

	@GetMapping("/quota/{userId}")
	@Operation(summary = "Get Quota Information",
			description = "Retrieves monthly quota usage for a user")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Quota info retrieved",
					content = @Content(schema = @Schema(implementation = QuotaDTO.class)))
	})
	public ResponseEntity<QuotaDTO> getQuota(@PathVariable Long userId) {
		log.info("Fetching quota info for user: {}", userId);
		QuotaDTO quota = aiService.getQuotaInfo(userId);
		return ResponseEntity.status(HttpStatus.OK).body(quota);
	}

	// Request DTOs
	public static class SummaryRequest {
		private Long userId;
		private Long resumeId;
		private String resumeContent;

		public Long getUserId() {
			return userId;
		}

		public void setUserId(Long userId) {
			this.userId = userId;
		}

		public Long getResumeId() {
			return resumeId;
		}

		public void setResumeId(Long resumeId) {
			this.resumeId = resumeId;
		}

		public String getResumeContent() {
			return resumeContent;
		}

		public void setResumeContent(String resumeContent) {
			this.resumeContent = resumeContent;
		}
	}

	public static class ImproveContentRequest {
		@NotNull
		private Long userId;
		private Long resumeId;
		@NotBlank
		private String text;
		@NotBlank
		private String type;
		private String context;

		public Long getUserId() {
			return userId;
		}

		public void setUserId(Long userId) {
			this.userId = userId;
		}

		public Long getResumeId() {
			return resumeId;
		}

		public void setResumeId(Long resumeId) {
			this.resumeId = resumeId;
		}

		public String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		public String getContext() {
			return context;
		}

		public void setContext(String context) {
			this.context = context;
		}
	}

	public static class ImproveContentResponse {
		private final String originalText;
		private final String improvedText;
		private final List<String> suggestions;
		private final double confidence;

		public ImproveContentResponse(String originalText, String improvedText, List<String> suggestions, double confidence) {
			this.originalText = originalText;
			this.improvedText = improvedText;
			this.suggestions = suggestions;
			this.confidence = confidence;
		}

		public String getOriginalText() {
			return originalText;
		}

		public String getImprovedText() {
			return improvedText;
		}

		public List<String> getSuggestions() {
			return suggestions;
		}

		public double getConfidence() {
			return confidence;
		}
	}

	public static class BulletsRequest {
		private Long userId;
		private Long resumeId;
		private String resumeContent;

		public Long getUserId() {
			return userId;
		}

		public void setUserId(Long userId) {
			this.userId = userId;
		}

		public Long getResumeId() {
			return resumeId;
		}

		public void setResumeId(Long resumeId) {
			this.resumeId = resumeId;
		}

		public String getResumeContent() {
			return resumeContent;
		}

		public void setResumeContent(String resumeContent) {
			this.resumeContent = resumeContent;
		}
	}

	public static class CoverLetterRequest {
		private Long userId;
		private Long resumeId;
		private String resumeContent;
		private String jobDescription;

		public Long getUserId() {
			return userId;
		}

		public void setUserId(Long userId) {
			this.userId = userId;
		}

		public Long getResumeId() {
			return resumeId;
		}

		public void setResumeId(Long resumeId) {
			this.resumeId = resumeId;
		}

		public String getResumeContent() {
			return resumeContent;
		}

		public void setResumeContent(String resumeContent) {
			this.resumeContent = resumeContent;
		}

		public String getJobDescription() {
			return jobDescription;
		}

		public void setJobDescription(String jobDescription) {
			this.jobDescription = jobDescription;
		}
	}

	public static class AtsRequest {
		private Long userId;
		private Long resumeId;
		private String resumeContent;
		private String jobDescription;

		public Long getUserId() {
			return userId;
		}

		public void setUserId(Long userId) {
			this.userId = userId;
		}

		public Long getResumeId() {
			return resumeId;
		}

		public void setResumeId(Long resumeId) {
			this.resumeId = resumeId;
		}

		public String getResumeContent() {
			return resumeContent;
		}

		public void setResumeContent(String resumeContent) {
			this.resumeContent = resumeContent;
		}

		public String getJobDescription() {
			return jobDescription;
		}

		public void setJobDescription(String jobDescription) {
			this.jobDescription = jobDescription;
		}
	}

	public static class TailorRequest {
		private Long userId;
		private Long resumeId;
		private String resumeJson;
		private String jobDescription;

		public Long getUserId() {
			return userId;
		}

		public void setUserId(Long userId) {
			this.userId = userId;
		}

		public Long getResumeId() {
			return resumeId;
		}

		public void setResumeId(Long resumeId) {
			this.resumeId = resumeId;
		}

		public String getResumeJson() {
			return resumeJson;
		}

		public void setResumeJson(String resumeJson) {
			this.resumeJson = resumeJson;
		}

		public String getJobDescription() {
			return jobDescription;
		}

		public void setJobDescription(String jobDescription) {
			this.jobDescription = jobDescription;
		}
	}
}

