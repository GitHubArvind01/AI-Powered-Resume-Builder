package com.resumeai.resume_section_service.mapper;

import com.resumeai.resume_section_service.dto.ResumeSectionRequestDTO;
import com.resumeai.resume_section_service.dto.ResumeSectionResponseDTO;
import com.resumeai.resume_section_service.entity.ResumeSection;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SectionMapper {

    public ResumeSection toEntity(ResumeSectionRequestDTO dto) {
        if (dto == null) {
            return null;
        }
        return ResumeSection.builder()
                .resumeId(dto.getResumeId())
                .sectionType(dto.getSectionType())
                .title(dto.getTitle())
                .content(dto.getContent())
                .displayOrder(dto.getDisplayOrder())
                .isVisible(dto.getIsVisible() != null ? dto.getIsVisible() : true)
                .aiGenerated(dto.getAiGenerated() != null ? dto.getAiGenerated() : false)
                .build();
    }

    public ResumeSectionResponseDTO toResponseDTO(ResumeSection entity) {
        if (entity == null) {
            return null;
        }
        return ResumeSectionResponseDTO.builder()
                .sectionId(entity.getSectionId())
                .resumeId(entity.getResumeId())
                .sectionType(entity.getSectionType())
                .title(entity.getTitle())
                .content(entity.getContent())
                .displayOrder(entity.getDisplayOrder())
                .isVisible(entity.getIsVisible())
                .aiGenerated(entity.getAiGenerated())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public List<ResumeSectionResponseDTO> toResponseDTOList(List<ResumeSection> entities) {
        if (entities == null) {
            return null;
        }
        return entities.stream()
                .map(this::toResponseDTO)
                .toList();
    }
}

