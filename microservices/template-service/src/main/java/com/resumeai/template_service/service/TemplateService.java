package com.resumeai.template_service.service;

import com.resumeai.template_service.dto.TemplateRequestDTO;
import com.resumeai.template_service.dto.TemplateResponseDTO;

import java.util.List;

public interface TemplateService {

    TemplateResponseDTO createTemplate(TemplateRequestDTO requestDTO);

    TemplateResponseDTO getTemplateById(Integer id);

    List<TemplateResponseDTO> getAllTemplates();

    List<TemplateResponseDTO> getFreeTemplates();

    List<TemplateResponseDTO> getPremiumTemplates();

    List<TemplateResponseDTO> getTemplatesByCategory(String category);

    List<TemplateResponseDTO> getPopularTemplates();

    TemplateResponseDTO updateTemplate(Integer id, TemplateRequestDTO requestDTO);

    void deactivateTemplate(Integer id);

    void incrementUsageCount(Integer id);
}

