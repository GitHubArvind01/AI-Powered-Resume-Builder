package com.resumeai.resume_service.service;

import java.util.List;

import com.resumeai.resume_service.dto.ResumeRequestDTO;
import com.resumeai.resume_service.dto.ResumeResponseDTO;

public interface ResumeService {

    /**
     * Create a new resume
     * @param resumeRequestDTO Resume data (without userId)
     * @param userId User ID (extracted from X-User-Id header by API Gateway)
     */
    ResumeResponseDTO createResume(ResumeRequestDTO resumeRequestDTO, Long userId);

    /**
     * Get resume by ID
     */
    ResumeResponseDTO getResumeById(Long id);

    /**
     * Get all resumes for a user
     */
    List<ResumeResponseDTO> getResumesByUserId(Long userId);

    /**
     * Get all public resumes
     */
    List<ResumeResponseDTO> getPublicResumes();

    /**
     * Update a resume
     * @param id Resume ID
     * @param resumeRequestDTO Updated resume data (without userId)
     * @param userId User ID (for ownership verification)
     */
    ResumeResponseDTO updateResume(Long id, ResumeRequestDTO resumeRequestDTO, Long userId);

    /**
     * Delete a resume
     * @param id Resume ID
     * @param userId User ID (for ownership verification)
     */
    void deleteResume(Long id, Long userId);

    /**
     * Duplicate (copy) a resume with "Copy of..." prefix in title
     */
    ResumeResponseDTO duplicateResume(Long id);

    /**
     * Publish/Unpublish a resume (make public/private)
     */
    ResumeResponseDTO publishResume(Long id, Boolean isPublic);

    /**
     * Increment view count for a resume
     */
    void incrementViewCount(Long id);

    /**
     * Get resumes by user ID and public status
     */
    List<ResumeResponseDTO> getResumesByUserIdAndPublic(Long userId, Boolean isPublic);

    /**
     * Count total resumes for a user
     */
    Integer countResumesByUserId(Long userId);
}

