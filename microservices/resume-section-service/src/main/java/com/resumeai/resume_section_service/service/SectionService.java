package com.resumeai.resume_section_service.service;

import com.resumeai.resume_section_service.dto.ResumeSectionRequestDTO;
import com.resumeai.resume_section_service.dto.ResumeSectionResponseDTO;

import java.util.List;

public interface SectionService {

    /**
     * Add a new resume section
     */
    ResumeSectionResponseDTO addSection(ResumeSectionRequestDTO requestDTO);

    /**
     * Get section by ID
     */
    ResumeSectionResponseDTO getSectionById(Long sectionId);

    /**
     * Get all sections for a resume
     */
    List<ResumeSectionResponseDTO> getSectionsByResume(Long resumeId);

    /**
     * Get sections by resume ID ordered by display order
     */
    List<ResumeSectionResponseDTO> getSectionsByResumeOrderByDisplayOrder(Long resumeId);

    /**
     * Get sections by type
     */
    List<ResumeSectionResponseDTO> getSectionsByType(Long resumeId, String sectionType);

    /**
     * Update a section
     */
    ResumeSectionResponseDTO updateSection(Long sectionId, ResumeSectionRequestDTO requestDTO);

    /**
     * Delete a section by ID
     */
    void deleteSection(Long sectionId);

    /**
     * Delete all sections for a resume
     */
    void deleteAllSectionsByResume(Long resumeId);

    /**
     * Toggle section visibility
     */
    ResumeSectionResponseDTO toggleVisibility(Long sectionId);

    /**
     * Reorder sections
     */
    void reorderSections(Long resumeId, List<Long> sectionIds);

    /**
     * Bulk update sections
     */
    List<ResumeSectionResponseDTO> bulkUpdateSections(List<ResumeSectionRequestDTO> requestDTOs);

    /**
     * Count sections for a resume
     */
    Long countSectionsByResume(Long resumeId);

    /**
     * Get AI-generated sections
     */
    List<ResumeSectionResponseDTO> getAiGeneratedSections(Long resumeId);
}

