package com.resumeai.template_service.service.impl;

import com.resumeai.template_service.dto.TemplateRequestDTO;
import com.resumeai.template_service.dto.TemplateResponseDTO;
import com.resumeai.template_service.entity.ResumeTemplate;
import com.resumeai.template_service.entity.TemplateCategory;
import com.resumeai.template_service.exception.ResourceNotFoundException;
import com.resumeai.template_service.mapper.TemplateMapper;
import com.resumeai.template_service.repository.TemplateRepository;
import com.resumeai.template_service.service.TemplateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional
public class TemplateServiceImpl implements TemplateService {

    private final TemplateRepository templateRepository;
    private final TemplateMapper templateMapper;

    public TemplateServiceImpl(TemplateRepository templateRepository, TemplateMapper templateMapper) {
        this.templateRepository = templateRepository;
        this.templateMapper = templateMapper;
    }

    @Override
    public TemplateResponseDTO createTemplate(TemplateRequestDTO requestDTO) {
        log.info("Creating new template with name: {}", requestDTO.getName());
        ResumeTemplate template = templateMapper.toEntity(requestDTO);
        ResumeTemplate savedTemplate = templateRepository.save(template);
        log.info("Template created successfully with ID: {}", savedTemplate.getTemplateId());
        return templateMapper.toDTO(savedTemplate);
    }

    @Override
    @Transactional(readOnly = true)
    public TemplateResponseDTO getTemplateById(Integer id) {
        log.info("Fetching template with ID: {}", id);
        ResumeTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found with ID: " + id));
        return templateMapper.toDTO(template);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TemplateResponseDTO> getAllTemplates() {
        log.info("Fetching all templates");
        return templateRepository.findAll()
                .stream()
                .map(templateMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TemplateResponseDTO> getFreeTemplates() {
        log.info("Fetching free templates");
        return templateRepository.findByIsPremium(false)
                .stream()
                .map(templateMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TemplateResponseDTO> getPremiumTemplates() {
        log.info("Fetching premium templates");
        return templateRepository.findByIsPremium(true)
                .stream()
                .map(templateMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TemplateResponseDTO> getTemplatesByCategory(String category) {
        log.info("Fetching templates by category: {}", category);
        TemplateCategory templateCategory = TemplateCategory.valueOf(category.toUpperCase().replace("-", "_"));
        return templateRepository.findByCategory(templateCategory)
                .stream()
                .map(templateMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TemplateResponseDTO> getPopularTemplates() {
        log.info("Fetching popular templates ordered by usage count");
        return templateRepository.findAllByOrderByUsageCountDesc()
                .stream()
                .map(templateMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public TemplateResponseDTO updateTemplate(Integer id, TemplateRequestDTO requestDTO) {
        log.info("Updating template with ID: {}", id);
        ResumeTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found with ID: " + id));
        
        templateMapper.updateEntityFromDTO(requestDTO, template);
        ResumeTemplate updatedTemplate = templateRepository.save(template);
        log.info("Template updated successfully with ID: {}", id);
        return templateMapper.toDTO(updatedTemplate);
    }

    @Override
    public void deactivateTemplate(Integer id) {
        log.info("Deactivating template with ID: {}", id);
        ResumeTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found with ID: " + id));
        
        template.setActive(false);
        templateRepository.save(template);
        log.info("Template deactivated successfully with ID: {}", id);
    }

    @Override
    public void incrementUsageCount(Integer id) {
        log.info("Incrementing usage count for template with ID: {}", id);
        ResumeTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found with ID: " + id));
        
        template.setUsageCount(template.getUsageCount() + 1);
        templateRepository.save(template);
        log.info("Usage count incremented for template with ID: {}", id);
    }
}

