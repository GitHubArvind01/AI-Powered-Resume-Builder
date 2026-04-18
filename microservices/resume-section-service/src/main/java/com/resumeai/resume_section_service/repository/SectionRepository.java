package com.resumeai.resume_section_service.repository;

import com.resumeai.resume_section_service.entity.ResumeSection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SectionRepository extends JpaRepository<ResumeSection, Long> {

    /**
     * Find all sections by resume ID
     */
    List<ResumeSection> findByResumeId(Long resumeId);

    /**
     * Find sections by resume ID and section type
     */
    List<ResumeSection> findByResumeIdAndSectionType(Long resumeId, String sectionType);

    /**
     * Find section by ID
     */
    Optional<ResumeSection> findBySectionId(Long sectionId);

    /**
     * Find all sections by resume ID ordered by display order
     */
    @Query("SELECT s FROM ResumeSection s WHERE s.resumeId = :resumeId ORDER BY s.displayOrder ASC")
    List<ResumeSection> findByResumeIdOrderByDisplayOrder(@Param("resumeId") Long resumeId);

    /**
     * Find all AI-generated sections
     */
    List<ResumeSection> findByAiGenerated(Boolean aiGenerated);

    /**
     * Count sections for a resume
     */
    Long countByResumeId(Long resumeId);

    /**
     * Delete all sections for a resume
     */
    void deleteByResumeId(Long resumeId);

    /**
     * Delete a specific section
     */
    void deleteBySectionId(Long sectionId);

    /**
     * Find all visible sections by resume ID
     */
    List<ResumeSection> findByResumeIdAndIsVisible(Long resumeId, Boolean isVisible);

    /**
     * Find all AI-generated sections for a resume
     */
    List<ResumeSection> findByResumeIdAndAiGenerated(Long resumeId, Boolean aiGenerated);
}

