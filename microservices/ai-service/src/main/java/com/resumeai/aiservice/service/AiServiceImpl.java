package com.resumeai.aiservice.service;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.resumeai.aiservice.client.GeminiClient;
import com.resumeai.aiservice.config.AiProviderConfig;
import com.resumeai.aiservice.dto.AiRequestDTO;
import com.resumeai.aiservice.dto.AtsReportDTO;
import com.resumeai.aiservice.dto.QuotaDTO;
import com.resumeai.aiservice.entity.AiRequest;
import com.resumeai.aiservice.entity.AiRequest.RequestStatus;
import com.resumeai.aiservice.entity.AiRequest.RequestType;
import com.resumeai.aiservice.exception.AiProviderException;
import com.resumeai.aiservice.exception.QuotaExceededException;
import com.resumeai.aiservice.exception.ResourceNotFoundException;
import com.resumeai.aiservice.mapper.AiRequestMapper;
import com.resumeai.aiservice.repository.AiRequestRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiServiceImpl implements AiService {

	private final AiRequestRepository aiRequestRepository;
	private final AiRequestMapper aiRequestMapper;
	private final GeminiClient geminiClient;
	private final AiProviderConfig aiProviderConfig;

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
	public AtsReportDTO checkAtsCompatibility(Long userId, Long resumeId, String resumeContent, String jobDescription)
			throws Exception {
		checkQuota(userId);

		String prompt = buildAtsPrompt(resumeContent, jobDescription);
		AiRequestDTO response = processAiRequest(userId, resumeId, RequestType.ATS, prompt);

		// Parse ATS response and compute scores
		return parseAtsResponse(userId, resumeId, response.getAiResponse(), jobDescription, resumeContent);
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
		Integer totalQuota = aiProviderConfig.getQuota() != null ? aiProviderConfig.getQuota().getFreeTierMonthlyLimit() : 100;
		Integer used = totalQuota - remaining;

		return QuotaDTO.builder().userId(userId).totalMonthlyQuota(totalQuota).usedQuota(used)
				.remainingQuota(remaining).tierType("FREE").build();
	}

	@Override
	public Integer getRemainingQuota(Long userId) {
		Integer totalQuota = aiProviderConfig.getQuota() != null ? aiProviderConfig.getQuota().getFreeTierMonthlyLimit() : 100;
		LocalDateTime startOfMonth = LocalDateTime.of(YearMonth.now().atDay(1), java.time.LocalTime.MIN);

		Integer usedQuota = aiRequestRepository.countByUserIdAndCreatedAtAfter(userId, startOfMonth);
		Integer remaining = Math.max(0, totalQuota - usedQuota);

		log.debug("User: {}, Total Quota: {}, Used: {}, Remaining: {}", userId, totalQuota, usedQuota, remaining);

		return remaining;
	}

	// Private helper methods
	private void checkQuota(Long userId) {
		Integer remainingQuota = getRemainingQuota(userId);
		if (remainingQuota <= 0) {
			throw new QuotaExceededException(
					"Monthly quota exceeded for user: " + userId + ". Please upgrade your plan.");
		}
		log.debug("Quota check passed for user: {}, remaining: {}", userId, remainingQuota);
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
		// Extract keywords from job description
		Set<String> jobKeywords = extractKeywords(jobDescription);
		Set<String> resumeKeywords = extractKeywords(resumeContent);

		// Calculate matches
		Set<String> matchedKeywords = resumeKeywords.stream().filter(jobKeywords::contains)
				.collect(Collectors.toSet());
		Set<String> missingKeywords = jobKeywords.stream().filter(k -> !resumeKeywords.contains(k))
				.collect(Collectors.toSet());

		// Calculate ATS score
		Integer atsScore = (int) (100.0 * matchedKeywords.size() / jobKeywords.size());
		atsScore = Math.min(100, Math.max(0, atsScore));

		List<String> improvements = new ArrayList<>();
		if (missingKeywords.size() > 0) {
			improvements.add("Add missing keywords: " + String.join(", ", missingKeywords.stream().limit(5)
					.collect(Collectors.toList())));
		}

		return AtsReportDTO.builder().userId(userId).resumeId(resumeId).atsScore(atsScore)
				.matchedKeywords(new ArrayList<>(matchedKeywords))
				.missingKeywords(new ArrayList<>(missingKeywords))
				.overallFeedback(aiResponse).improvements(improvements)
				.totalKeywordsChecked(jobKeywords.size()).keywordsMatched(matchedKeywords.size()).build();
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

	private String buildAtsPrompt(String resumeContent, String jobDescription) {
		return "Analyze the following resume for ATS compatibility with the job description. "
				+ "Identify keywords and suggestions:\n\nResume:\n" + resumeContent + "\n\nJob Description:\n"
				+ jobDescription;
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
}


