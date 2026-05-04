package com.resumeai.aiservice.service;

import java.util.List;
import java.util.UUID;

import com.resumeai.aiservice.dto.AiRequestDTO;
import com.resumeai.aiservice.dto.AtsReportDTO;
import com.resumeai.aiservice.dto.QuotaDTO;
import com.resumeai.aiservice.entity.AiRequest;

public interface AiService {

	/**
	 * Generate a professional summary for a resume
	 */
	AiRequestDTO generateSummary(Long userId, Long resumeId, String resumeContent) throws Exception;

	/**
	 * Generate achievement bullets for a resume
	 */
	AiRequestDTO generateBullets(Long userId, Long resumeId, String resumeContent) throws Exception;

	/**
	 * Generate a tailored cover letter
	 */
	AiRequestDTO generateCoverLetter(Long userId, Long resumeId, String resumeContent, String jobDescription)
			throws Exception;

	/**
	 * Improve resume content
	 */
	AiRequestDTO improveResume(Long userId, Long resumeId, String resumeContent) throws Exception;

	/**
	 * Check ATS compatibility with keyword matching and semantic similarity
	 */
	AtsReportDTO checkAtsCompatibility(Long userId, Long resumeId, String resumeContent, String jobDescription)
			throws Exception;

	/**
	 * Extract skills from resume
	 */
	AiRequestDTO extractSkills(Long userId, Long resumeId, String resumeContent) throws Exception;

	/**
	 * Tailor entire resume for a specific job description
	 */
	AiRequestDTO tailorResumeForJob(Long userId, Long resumeId, String resumeJson, String jobDescription)
			throws Exception;

	/**
	 * Translate resume content
	 */
	AiRequestDTO translateResume(Long userId, Long resumeId, String resumeContent, String targetLanguage)
			throws Exception;

	/**
	 * Get request history for a user
	 */
	List<AiRequestDTO> getRequestHistory(Long userId);

	/**
	 * Get quota information for a user
	 */
	QuotaDTO getQuotaInfo(Long userId);

	/**
	 * Get remaining monthly quota for a user
	 */
	Integer getRemainingQuota(Long userId);
}

