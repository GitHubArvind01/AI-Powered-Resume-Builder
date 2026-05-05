package com.resumeai.aiservice.service;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Service;

import com.resumeai.aiservice.client.AuthUserClient;
import com.resumeai.aiservice.client.GeminiClient;
import com.resumeai.aiservice.config.AiProviderConfig;
import com.resumeai.aiservice.dto.AiRequestDTO;
import com.resumeai.aiservice.dto.AiAssistantResponseDTO;
import com.resumeai.aiservice.dto.AtsReportDTO;
import com.resumeai.aiservice.dto.AuthUserProfileDTO;
import com.resumeai.aiservice.dto.QuotaDTO;
import com.resumeai.aiservice.dto.SimpleAtsResponseDTO;
import com.resumeai.aiservice.entity.AiRequest;
import com.resumeai.aiservice.entity.AiRequest.RequestStatus;
import com.resumeai.aiservice.entity.AiRequest.RequestType;
import com.resumeai.aiservice.exception.AiProviderException;
import com.resumeai.aiservice.exception.QuotaExceededException;
import com.resumeai.aiservice.exception.ResourceNotFoundException;
import com.resumeai.aiservice.mapper.AiRequestMapper;
import com.resumeai.aiservice.repository.AiRequestRepository;
import com.resumeai.aiservice.service.ResumeTextExtractionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiServiceImpl implements AiService {

	private final AiRequestRepository aiRequestRepository;
	private final AiRequestMapper aiRequestMapper;
	private final GeminiClient geminiClient;
	private final AiProviderConfig aiProviderConfig;
	private final ResumeTextExtractionService resumeTextExtractionService;
	private final AuthUserClient authUserClient;

	@Override
	public AiRequestDTO generateSummary(Long userId, Long resumeId, String resumeContent) throws Exception {
		checkQuota(userId);

		String prompt = buildSummaryPrompt(resumeContent);
		return processAiRequest(userId, resumeId, RequestType.SUMMARY, prompt);
	}

	@Override
	public AiRequestDTO generateBullets(Long userId, Long resumeId, String resumeContent) throws Exception {
		checkQuota(userId);

		String prompt = buildBulletsPrompt(resumeContent);
		return processAiRequest(userId, resumeId, RequestType.BULLETS, prompt);
	}

	@Override
	public AiRequestDTO generateCoverLetter(Long userId, Long resumeId, String resumeContent, String jobDescription)
			throws Exception {
		checkQuota(userId);

		String prompt = buildCoverLetterPrompt(resumeContent, jobDescription);
		return processAiRequest(userId, resumeId, RequestType.COVER_LETTER, prompt);
	}

	@Override
	public AiRequestDTO improveResume(Long userId, Long resumeId, String resumeContent) throws Exception {
		checkQuota(userId);

		String prompt = buildImprovePrompt(resumeContent);
		return processAiRequest(userId, resumeId, RequestType.IMPROVE, prompt);
	}

	@Override
	public AiRequestDTO generateSectionContent(Long userId, Long resumeId, String sectionType, String context)
			throws Exception {
		checkQuota(userId);
		return processAiRequest(userId, resumeId, RequestType.IMPROVE, buildGeneratePrompt(sectionType, context));
	}

	@Override
	public AtsReportDTO checkAtsCompatibility(Long userId, Long resumeId, String resumeContent, String jobDescription)
			throws Exception {
		checkQuota(userId);

		String prompt = buildAtsPrompt(resumeContent, jobDescription);
		String aiResponseText = "";

		try {
			AiRequestDTO response = processAiRequest(userId, resumeId, RequestType.ATS, prompt);
			aiResponseText = response.getAiResponse();
		} catch (AiProviderException e) {
			log.warn("Gemini is down (503). Falling back to local keyword-bag algorithm for ATS check.");
			// aiResponseText remains empty, which will force parseAtsResponse to use the local fallback
			aiResponseText = "AI Service temporarily unavailable. Using local fallback algorithm.";
		}

		// Parse ATS response and compute scores (If AI failed, this will now safely run Stage 2 fallback)
		return parseAtsResponse(userId, resumeId, aiResponseText, jobDescription, resumeContent);
	}

	@Override
	public SimpleAtsResponseDTO analyzeResume(Long userId, MultipartFile file) throws Exception {
		String resumeContent = resumeTextExtractionService.extractText(file);

		AtsReportDTO report = checkAtsCompatibility(userId, null, resumeContent, "");
		List<String> suggestions = new ArrayList<>(report.getImprovements() == null ? List.of() : report.getImprovements());

		if (suggestions.isEmpty() && report.getOverallFeedback() != null && !report.getOverallFeedback().isBlank()) {
			suggestions.add(report.getOverallFeedback());
		}

		return SimpleAtsResponseDTO.builder()
				.score(report.getAtsScore())
				.keywordMatchPercentage(calculateKeywordMatchPercentage(report))
				.keywordsMatched(report.getMatchedKeywords() == null ? List.of() : report.getMatchedKeywords())
				.missingKeywords(report.getMissingKeywords() == null ? List.of() : report.getMissingKeywords())
				.suggestions(suggestions)
				.build();
	}

	@Override
	public AiRequestDTO extractSkills(Long userId, Long resumeId, String resumeContent) throws Exception {
		checkQuota(userId);

		String prompt = buildSkillsPrompt(resumeContent);
		return processAiRequest(userId, resumeId, RequestType.SKILLS, prompt);
	}

	@Override
	public AiRequestDTO tailorResumeForJob(Long userId, Long resumeId, String resumeJson, String jobDescription)
			throws Exception {
		checkQuota(userId);

		String prompt = buildTailorPrompt(resumeJson, jobDescription);
		AiRequestDTO result = processAiRequest(userId, resumeId, RequestType.TAILOR, prompt);

		log.info("Resume tailoring completed for user: {}, resumeId: {}", userId, resumeId);
		return result;
	}

	@Override
	public AiRequestDTO translateResume(Long userId, Long resumeId, String resumeContent, String targetLanguage)
			throws Exception {
		checkQuota(userId);

		String prompt = buildTranslatePrompt(resumeContent, targetLanguage);
		return processAiRequest(userId, resumeId, RequestType.TRANSLATE, prompt);
	}

	@Override
	public List<AiRequestDTO> getRequestHistory(Long userId) {
		List<AiRequest> history = aiRequestRepository.findByUserIdOrderByCreatedAtDesc(userId);
		if (history.isEmpty()) {
			throw new ResourceNotFoundException("No request history found for user: " + userId);
		}
		return history.stream().map(aiRequestMapper::toDTO).collect(Collectors.toList());
	}

	@Override
	public QuotaDTO getQuotaInfo(Long userId) {
		Integer remaining = getRemainingQuota(userId);
		Integer totalQuota = getQuotaLimit(userId);
		Integer used = totalQuota - remaining;
		String tier = resolveTier(userId);

		return QuotaDTO.builder().userId(userId).totalMonthlyQuota(totalQuota).usedQuota(used)
				.remainingQuota(remaining).tierType(tier).build();
	}

	@Override
	public Integer getRemainingQuota(Long userId) {
		Integer totalQuota = getQuotaLimit(userId);
		LocalDateTime startOfMonth = LocalDateTime.of(YearMonth.now().atDay(1), java.time.LocalTime.MIN);

		Integer usedQuota = aiRequestRepository.countByUserIdAndCreatedAtAfter(userId, startOfMonth);
		Integer remaining = Math.max(0, totalQuota - usedQuota);

		log.debug("User: {}, Total Quota: {}, Used: {}, Remaining: {}", userId, totalQuota, usedQuota, remaining);

		return remaining;
	}

	@Override
	public AiAssistantResponseDTO buildAssistantResponse(AiRequestDTO aiRequestDTO, Long userId) {
		Integer remaining = getRemainingQuota(userId);
		return AiAssistantResponseDTO.builder()
				.content(aiRequestDTO.getAiResponse())
				.remainingUsage(remaining)
				.limitReached(remaining <= 0)
				.suggestions(List.of("Tailor the generated draft to measurable outcomes before saving."))
				.build();
	}

	// Private helper methods
	private void checkQuota(Long userId) {
		Integer remainingQuota = getRemainingQuota(userId);
		if (remainingQuota <= 0) {
			throw new QuotaExceededException(
					"AI request limit reached for this account. Free users can make 5 AI requests. Please upgrade your plan to continue.");
		}
		log.debug("Quota check passed for user: {}, remaining: {}", userId, remainingQuota);
	}

	private Integer getQuotaLimit(Long userId) {
		String tier = resolveTier(userId);
		AiProviderConfig.QuotaConfig quotaConfig = aiProviderConfig.getQuota();
		if (!"FREE".equals(tier)) {
			return quotaConfig != null && quotaConfig.getPremiumMonthlyLimit() != null
					? quotaConfig.getPremiumMonthlyLimit()
					: 500;
		}

		return quotaConfig != null && quotaConfig.getFreeTierMonthlyLimit() != null
				? quotaConfig.getFreeTierMonthlyLimit()
				: 5;
	}

	private String resolveTier(Long userId) {
		try {
			AuthUserProfileDTO user = authUserClient.getUserById(userId);
			String subscriptionPlan = user != null && user.getSubscriptionPlan() != null
					? user.getSubscriptionPlan().toUpperCase(Locale.ROOT)
					: "FREE";
			return "FREE".equals(subscriptionPlan) ? "FREE" : subscriptionPlan;
		} catch (Exception exception) {
			log.warn("Unable to resolve subscription plan for user {}. Falling back to FREE tier.", userId);
			return "FREE";
		}
	}

	private AiRequestDTO processAiRequest(Long userId, Long resumeId, RequestType requestType, String prompt)
			throws Exception {
		AiRequest aiRequest = AiRequest.builder().userId(userId).resumeId(resumeId).requestType(requestType)
				.inputPrompt(prompt).status(RequestStatus.PROCESSING).build();

		try {
			// Use Gemini as primary provider
			String response;
			String model;

			if (geminiClient.isAvailable()) {
				try {
					response = geminiClient.callAiProvider(prompt);
					model = geminiClient.getModelName();
					log.info("Gemini response received for request type: {}", requestType);
				} catch (Exception e) {
					log.warn("Gemini failed: {}", e.getMessage());
					throw new AiProviderException("No fallback provider available after Gemini failed");
				}
			} else {
				throw new AiProviderException("Gemini AI provider is not available");
			}

			aiRequest.setAiResponse(response);
			aiRequest.setModel(model);
			aiRequest.setStatus(RequestStatus.COMPLETED);
			aiRequest.setCompletedAt(LocalDateTime.now());
			aiRequest.setTokensUsed(estimateTokens(response));

		} catch (Exception e) {
			log.error("Error processing AI request for user: {}, type: {}", userId, requestType, e);
			aiRequest.setStatus(RequestStatus.FAILED);
			// Truncate error message to prevent database column overflow
			String errorMsg = e.getMessage();
			if (errorMsg != null && errorMsg.length() > 2000) {
				errorMsg = errorMsg.substring(0, 2000) + "... (truncated)";
			}
			aiRequest.setErrorMessage(errorMsg);
			aiRequest.setCompletedAt(LocalDateTime.now());
			aiRequestRepository.save(aiRequest);
			throw new AiProviderException("Failed to process AI request: " + e.getMessage(), e);
		}

		AiRequest savedRequest = aiRequestRepository.save(aiRequest);
		log.info("AI request saved for user: {}, requestId: {}, type: {}", userId, savedRequest.getRequestId(),
				requestType);

		return aiRequestMapper.toDTO(savedRequest);
	}

	private AtsReportDTO parseAtsResponse(Long userId, Long resumeId, String aiResponse, String jobDescription,
			String resumeContent) {

		// --- Stage 1: Try to parse the structured JSON the prompt requests ---
		try {
			// Gemini may wrap JSON in a markdown code block; strip it
			String json = aiResponse.trim();
			Pattern codeBlock = Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);
			Matcher matcher = codeBlock.matcher(json);
			if (matcher.find()) {
				json = matcher.group(1).trim();
			}

			// Also handle JSON embedded anywhere after non-JSON preamble
			int braceStart = json.indexOf('{');
			int braceEnd = json.lastIndexOf('}');
			if (braceStart >= 0 && braceEnd > braceStart) {
				json = json.substring(braceStart, braceEnd + 1);
			}

			ObjectMapper mapper = new ObjectMapper();
			JsonNode root = mapper.readTree(json);

			int atsScore = root.path("atsScore").asInt(0);
			atsScore = Math.min(100, Math.max(0, atsScore));

			List<String> matched = new ArrayList<>();
			root.path("matchedKeywords").forEach(node -> matched.add(node.asText()));

			List<String> missing = new ArrayList<>();
			root.path("missingKeywords").forEach(node -> missing.add(node.asText()));

			List<String> improvements = new ArrayList<>();
			root.path("improvements").forEach(node -> improvements.add(node.asText()));

			String feedback = root.path("overallFeedback").asText(
					"Analysis complete. Review the matched and missing keywords above.");

			log.info("[ATS] Successfully parsed structured JSON response. score={}", atsScore);

			return AtsReportDTO.builder()
					.userId(userId)
					.resumeId(resumeId)
					.atsScore(atsScore)
					.matchedKeywords(matched)
					.missingKeywords(missing)
					.improvements(improvements)
					.overallFeedback(feedback)
					.totalKeywordsChecked(matched.size() + missing.size())
					.keywordsMatched(matched.size())
					.build();

		} catch (Exception parseException) {
			log.warn("[ATS] Could not parse JSON from AI response ({}). Falling back to keyword-match algorithm.",
					parseException.getMessage());
		}

		// --- Stage 2: Fallback — simple keyword-bag algorithm ---
		Set<String> jobKeywords = extractKeywords(jobDescription);
		Set<String> resumeKeywords = extractKeywords(resumeContent);

		if (jobKeywords.isEmpty()) {
			// No JD provided — score based on resume structure quality heuristic
			int heuristicScore = computeStructureScore(resumeContent);
			return AtsReportDTO.builder()
					.userId(userId)
					.resumeId(resumeId)
					.atsScore(heuristicScore)
					.matchedKeywords(new ArrayList<>())
					.missingKeywords(new ArrayList<>())
					.overallFeedback(aiResponse)
					.improvements(List.of(
							"Add a target job description for a more precise keyword-match score.",
							"Ensure your resume uses action verbs and quantifiable achievements.",
							"Include measurable impact in each experience bullet (e.g., \"Reduced load time by 40%\")."))
					.totalKeywordsChecked(0)
					.keywordsMatched(0)
					.build();
		}

		Set<String> matchedKeywords = resumeKeywords.stream().filter(jobKeywords::contains)
				.collect(Collectors.toSet());
		Set<String> missingKeywords = jobKeywords.stream().filter(k -> !resumeKeywords.contains(k))
				.collect(Collectors.toSet());

		int atsScore = (int) (100.0 * matchedKeywords.size() / jobKeywords.size());
		atsScore = Math.min(100, Math.max(0, atsScore));

		List<String> improvements = new ArrayList<>();
		if (!missingKeywords.isEmpty()) {
			improvements.add("Add missing keywords: " + String.join(", ",
					missingKeywords.stream().limit(5).collect(Collectors.toList())));
		}

		return AtsReportDTO.builder()
				.userId(userId)
				.resumeId(resumeId)
				.atsScore(atsScore)
				.matchedKeywords(new ArrayList<>(matchedKeywords))
				.missingKeywords(new ArrayList<>(missingKeywords))
				.overallFeedback(aiResponse)
				.improvements(improvements)
				.totalKeywordsChecked(jobKeywords.size())
				.keywordsMatched(matchedKeywords.size())
				.build();
	}

	/**
	 * Heuristic resume-quality score (0–100) used when no job description is provided.
	 * Rewards presence of key resume sections and penalises very short content.
	 */
	private int computeStructureScore(String resumeContent) {
		if (resumeContent == null || resumeContent.trim().isEmpty()) return 0;
		int score = 40; // base
		String lower = resumeContent.toLowerCase();
		if (lower.contains("experience") || lower.contains("worked") || lower.contains("responsible")) score += 15;
		if (lower.contains("education") || lower.contains("university") || lower.contains("degree")) score += 10;
		if (lower.contains("skill") || lower.contains("proficient") || lower.contains("expertise")) score += 10;
		if (lower.contains("achieved") || lower.contains("improved") || lower.contains("%") || lower.contains("reduced")) score += 15;
		if (resumeContent.length() < 300) score -= 20;
		return Math.min(100, Math.max(0, score));
	}

	private Set<String> extractKeywords(String content) {
		if (content == null || content.isEmpty()) {
			return new HashSet<>();
		}

		// Simple keyword extraction - can be enhanced with NLP
		String[] words = content.toLowerCase().replaceAll("[^a-z0-9\\s+#]", " ").split("\\s+");

		Set<String> keywords = new HashSet<>();
		for (String word : words) {
			if (word.length() > 3) { // Only words > 3 chars
				keywords.add(word);
			}
		}

		return keywords;
	}

	private Integer estimateTokens(String text) {
		// Rough estimate: ~4 characters per token
		return Math.max(1, text.length() / 4);
	}

	// Prompt builders
	private String buildSummaryPrompt(String resumeContent) {
		return "Rewrite the following resume summary into a more professional, concise, and impactful paragraph. "
				+ "Do not explain anything. Only return the improved summary. "
				+ "The result must be a single paragraph with no headings, bullet points, or suggestions.\n\n"
				+ resumeContent;
	}

	private String buildBulletsPrompt(String resumeContent) {
		return "Extract and optimize achievement bullets from the following resume content. "
				+ "Format as bullet points with action verbs:\n\n" + resumeContent;
	}

	private String buildCoverLetterPrompt(String resumeContent, String jobDescription) {
		return "Generate a professional cover letter based on the resume and job description:\n\n"
				+ "Resume:\n" + resumeContent + "\n\nJob Description:\n" + jobDescription;
	}

	private String buildImprovePrompt(String resumeContent) {
		return "Rewrite the following resume content to be more professional, concise, and impactful. "
				+ "Do not explain your changes. Return only the improved final text with no headings or bullet points unless the input is already bullet content.\n\n"
				+ resumeContent;
	}

	private String buildGeneratePrompt(String sectionType, String context) {
		return "Generate premium, ATS-friendly resume content for the " + sectionType + " section. "
				+ "Use concise, credible language with measurable impact where appropriate. "
				+ "Return only the final content without commentary.\n\nContext:\n" + context;
	}

	private String buildAtsPrompt(String resumeContent, String jobDescription) {
		boolean hasJobDescription = jobDescription != null && !jobDescription.trim().isEmpty();

		String analysisInstruction = hasJobDescription
				? "Analyze the resume against the provided job description. Identify matched keywords, missing keywords, and score accordingly."
				: "No job description was provided. Analyze the resume based on general industry best practices: "
						+ "use of action verbs, quantifiable achievements, proper structure (summary, experience, education, skills), "
						+ "keyword density, and ATS-friendliness. Score accordingly (0-100).";

		return "You are an expert ATS (Applicant Tracking System) analyst. " + analysisInstruction + "\n\n"
				+ "Resume Content:\n" + resumeContent + "\n\n"
				+ (hasJobDescription ? "Job Description:\n" + jobDescription + "\n\n" : "")
				+ "You MUST respond with ONLY a valid JSON object. Do not include any explanation or markdown. "
				+ "The JSON must use exactly this structure:\n"
				+ "{\n"
				+ "  \"atsScore\": <integer 0-100>,\n"
				+ "  \"matchedKeywords\": [<list of keyword strings found in both resume and job description>],\n"
				+ "  \"missingKeywords\": [<list of important keywords from the job description missing in resume>],\n"
				+ "  \"improvements\": [<list of 3-5 specific, actionable improvement suggestions as strings>],\n"
				+ "  \"overallFeedback\": \"<2-3 sentence overall assessment string>\"\n"
				+ "}";
	}

	private String buildSkillsPrompt(String resumeContent) {
		return "Extract and list all technical and soft skills from the following resume:\n\n" + resumeContent;
	}

	private String buildTailorPrompt(String resumeJson, String jobDescription) {
		return "Tailor the following resume JSON to match the job description. Return the modified resume as JSON:\n\n"
				+ "Resume JSON:\n" + resumeJson + "\n\nJob Description:\n" + jobDescription;
	}

	private String buildTranslatePrompt(String resumeContent, String targetLanguage) {
		return "Translate the following resume content to " + targetLanguage
				+ ". Maintain professional formatting:\n\n" + resumeContent;
	}

	private Integer calculateKeywordMatchPercentage(AtsReportDTO report) {
		int total = report.getTotalKeywordsChecked() == null ? 0 : report.getTotalKeywordsChecked();
		int matched = report.getKeywordsMatched() == null ? 0 : report.getKeywordsMatched();
		if (total <= 0) {
			return report.getAtsScore();
		}
		return Math.min(100, Math.max(0, (int) Math.round((matched * 100.0) / total)));
	}
}


