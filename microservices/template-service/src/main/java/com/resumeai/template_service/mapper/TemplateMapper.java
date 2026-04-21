package com.resumeai.template_service.mapper;

import com.resumeai.template_service.dto.TemplateRequestDTO;
import com.resumeai.template_service.dto.TemplateResponseDTO;
import com.resumeai.template_service.entity.ResumeTemplate;
import com.resumeai.template_service.entity.TemplateCategory;
import org.springframework.stereotype.Component;

@Component
public class TemplateMapper {

    public ResumeTemplate toEntity(TemplateRequestDTO dto) {
        return ResumeTemplate.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .thumbnailUrl(dto.getThumbnailUrl())
                .htmlLayout(dto.getHtmlLayout())
                .cssStyles(dto.getCssStyles())
                .category(TemplateCategory.valueOf(dto.getCategory().toUpperCase().replace("-", "_")))
                .isPremium(dto.isPremium())
                .isActive(dto.isActive())
                .usageCount(0)
                .build();
    }

    public TemplateResponseDTO toDTO(ResumeTemplate entity) {
        return TemplateResponseDTO.builder()
                .templateId(entity.getTemplateId())
                .name(entity.getName())
                .description(entity.getDescription())
                .thumbnailUrl(entity.getThumbnailUrl())
                .htmlLayout(entity.getHtmlLayout())
                .cssStyles(entity.getCssStyles())
                .category(entity.getCategory().name())
                .isPremium(entity.isPremium())
                .isActive(entity.isActive())
                .usageCount(entity.getUsageCount())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public void updateEntityFromDTO(TemplateRequestDTO dto, ResumeTemplate entity) {
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setThumbnailUrl(dto.getThumbnailUrl());
        entity.setHtmlLayout(dto.getHtmlLayout());
        entity.setCssStyles(dto.getCssStyles());
        entity.setCategory(TemplateCategory.valueOf(dto.getCategory().toUpperCase().replace("-", "_")));
        entity.setPremium(dto.isPremium());
        entity.setActive(dto.isActive());
    }
}

