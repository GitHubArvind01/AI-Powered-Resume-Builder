package com.resumeai.resume_section_service.service.impl;

import com.resumeai.resume_section_service.dto.ResumeSectionRequestDTO;
import com.resumeai.resume_section_service.dto.ResumeSectionResponseDTO;
import com.resumeai.resume_section_service.entity.ResumeSection;
import com.resumeai.resume_section_service.exception.ResourceNotFoundException;
import com.resumeai.resume_section_service.exception.SectionServiceException;
import com.resumeai.resume_section_service.mapper.SectionMapper;
import com.resumeai.resume_section_service.repository.SectionRepository;
import com.resumeai.resume_section_service.service.SectionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional
public class SectionServiceImpl implements SectionService {

    private final SectionRepository sectionRepository;
    private final SectionMapper sectionMapper;

    public SectionServiceImpl(SectionRepository sectionRepository, SectionMapper sectionMapper) {
        this.sectionRepository = sectionRepository;
        this.sectionMapper = sectionMapper;
    }

    @Override
    public ResumeSectionResponseDTO addSection(ResumeSectionRequestDTO requestDTO) {
        log.info("Adding new section for resume: {}", requestDTO.getResumeId());

        try {
            ResumeSection section = sectionMapper.toEntity(requestDTO);
            ResumeSection savedSection = sectionRepository.save(section);

            log.info("Section added successfully with ID: {}", savedSection.getSectionId());
            return sectionMapper.toResponseDTO(savedSection);
        } catch (Exception e) {
            log.error("Error adding section", e);
            throw new SectionServiceException("Failed to add section: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ResumeSectionResponseDTO getSectionById(Long sectionId) {
        log.info("Fetching section with ID: {}", sectionId);

        ResumeSection section = sectionRepository.findBySectionId(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Section not found with ID: " + sectionId));

        return sectionMapper.toResponseDTO(section);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResumeSectionResponseDTO> getSectionsByResume(Long resumeId) {
        log.info("Fetching all sections for resume: {}", resumeId);

        List<ResumeSection> sections = sectionRepository.findByResumeId(resumeId);
        return sectionMapper.toResponseDTOList(sections);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResumeSectionResponseDTO> getSectionsByResumeOrderByDisplayOrder(Long resumeId) {
        log.info("Fetching sections for resume: {} ordered by display order", resumeId);

        List<ResumeSection> sections = sectionRepository.findByResumeIdOrderByDisplayOrder(resumeId);
        return sectionMapper.toResponseDTOList(sections);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResumeSectionResponseDTO> getSectionsByType(Long resumeId, String sectionType) {
        log.info("Fetching sections for resume: {} with type: {}", resumeId, sectionType);

        List<ResumeSection> sections = sectionRepository.findByResumeIdAndSectionType(resumeId, sectionType);
        return sectionMapper.toResponseDTOList(sections);
    }

    @Override
    public ResumeSectionResponseDTO updateSection(Long sectionId, ResumeSectionRequestDTO requestDTO) {
        log.info("Updating section with ID: {}", sectionId);

        ResumeSection section = sectionRepository.findBySectionId(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Section not found with ID: " + sectionId));

        section.setSectionType(requestDTO.getSectionType());
        section.setTitle(requestDTO.getTitle());
        section.setContent(requestDTO.getContent());
        section.setDisplayOrder(requestDTO.getDisplayOrder());
        section.setIsVisible(requestDTO.getIsVisible() != null ? requestDTO.getIsVisible() : true);
        section.setAiGenerated(requestDTO.getAiGenerated() != null ? requestDTO.getAiGenerated() : false);

        ResumeSection updatedSection = sectionRepository.save(section);

        log.info("Section updated successfully with ID: {}", sectionId);
        return sectionMapper.toResponseDTO(updatedSection);
    }

    @Override
    public void deleteSection(Long sectionId) {
        log.info("Deleting section with ID: {}", sectionId);

        ResumeSection section = sectionRepository.findBySectionId(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Section not found with ID: " + sectionId));

        sectionRepository.delete(section);

        log.info("Section deleted successfully with ID: {}", sectionId);
    }

    @Override
    public void deleteAllSectionsByResume(Long resumeId) {
        log.info("Deleting all sections for resume: {}", resumeId);

        sectionRepository.deleteByResumeId(resumeId);

        log.info("All sections deleted for resume: {}", resumeId);
    }

    @Override
    public ResumeSectionResponseDTO toggleVisibility(Long sectionId) {
        log.info("Toggling visibility for section: {}", sectionId);

        ResumeSection section = sectionRepository.findBySectionId(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Section not found with ID: " + sectionId));

        section.setIsVisible(!section.getIsVisible());
        ResumeSection updatedSection = sectionRepository.save(section);

        log.info("Section visibility toggled for ID: {}", sectionId);
        return sectionMapper.toResponseDTO(updatedSection);
    }

    @Override
    public void reorderSections(Long resumeId, List<Long> sectionIds) {
        log.info("Reordering sections for resume: {}", resumeId);

        List<ResumeSection> sections = sectionRepository.findByResumeId(resumeId);

        if (sections.isEmpty()) {
            throw new SectionServiceException("No sections found for resume: " + resumeId);
        }

        for (int i = 0; i < sectionIds.size(); i++) {
            Long sectionId = sectionIds.get(i);
            ResumeSection section = sections.stream()
                    .filter(s -> s.getSectionId().equals(sectionId))
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Section not found with ID: " + sectionId));

            section.setDisplayOrder(i);
            sectionRepository.save(section);
        }

        log.info("Sections reordered successfully for resume: {}", resumeId);
    }

    @Override
    public List<ResumeSectionResponseDTO> bulkUpdateSections(List<ResumeSectionRequestDTO> requestDTOs) {
        log.info("Bulk updating {} sections", requestDTOs.size());

        List<ResumeSection> sections = requestDTOs.stream()
                .map(dto -> {
                    Long sectionId = dto.getResumeId();
                    ResumeSection section = sectionRepository.findBySectionId(sectionId)
                            .orElseThrow(() -> new ResourceNotFoundException(
                                    "Section not found with ID: " + sectionId));

                    section.setSectionType(dto.getSectionType());
                    section.setTitle(dto.getTitle());
                    section.setContent(dto.getContent());
                    section.setDisplayOrder(dto.getDisplayOrder());
                    section.setIsVisible(dto.getIsVisible() != null ? dto.getIsVisible() : true);
                    section.setAiGenerated(dto.getAiGenerated() != null ? dto.getAiGenerated() : false);

                    return section;
                })
                .toList();

        List<ResumeSection> savedSections = sectionRepository.saveAll(sections);

        log.info("Bulk update completed for {} sections", savedSections.size());
        return sectionMapper.toResponseDTOList(savedSections);
    }

    @Override
    @Transactional(readOnly = true)
    public Long countSectionsByResume(Long resumeId) {
        log.info("Counting sections for resume: {}", resumeId);
        return sectionRepository.countByResumeId(resumeId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResumeSectionResponseDTO> getAiGeneratedSections(Long resumeId) {
        log.info("Fetching AI-generated sections for resume: {}", resumeId);

        List<ResumeSection> sections = sectionRepository.findByResumeIdAndAiGenerated(resumeId, true);
        return sectionMapper.toResponseDTOList(sections);
    }
}
