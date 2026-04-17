package com.resumeai.resume_service.service;

import java.util.List;

import com.resumeai.resume_service.dto.ResumeRequestDTO;
import com.resumeai.resume_service.dto.ResumeResponseDTO;

public interface ResumeService {

    /**
     * Create a new resume
     */
    ResumeResponseDTO createResume(ResumeRequestDTO resumeRequestDTO);

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
     */
    ResumeResponseDTO updateResume(Long id, ResumeRequestDTO resumeRequestDTO);

    /**
     * Delete a resume
     */
    void deleteResume(Long id);

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

