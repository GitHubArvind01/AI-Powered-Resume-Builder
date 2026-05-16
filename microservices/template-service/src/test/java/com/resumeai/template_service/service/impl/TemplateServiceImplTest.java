package com.resumeai.template_service.service;

import com.resumeai.template_service.dto.TemplateRequestDTO;
import com.resumeai.template_service.dto.TemplateResponseDTO;
import com.resumeai.template_service.entity.ResumeTemplate;
import com.resumeai.template_service.entity.TemplateCategory;
import com.resumeai.template_service.exception.ResourceNotFoundException;
import com.resumeai.template_service.mapper.TemplateMapper;
import com.resumeai.template_service.repository.TemplateRepository;
import com.resumeai.template_service.service.impl.TemplateServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TemplateServiceImplTest {

    @Mock
    private TemplateRepository templateRepository;

    @Mock
    private TemplateMapper templateMapper;

    @InjectMocks
    private TemplateServiceImpl templateService;

    private ResumeTemplate mockTemplate;
    private TemplateRequestDTO mockRequestDTO;
    private TemplateResponseDTO mockResponseDTO;

    @BeforeEach
    void setUp() {
        mockTemplate = ResumeTemplate.builder()
                .templateId(1)
                .name("Professional Template")
                .description("A professional resume template")
                .htmlLayout("<html>...</html>")
                .cssStyles("body { font-family: Arial; }")
                .category(TemplateCategory.PROFESSIONAL)
                .isPremium(false)
                .isActive(true)
                .usageCount(10)
                .createdAt(LocalDateTime.now())
                .build();

        mockRequestDTO = TemplateRequestDTO.builder()
                .name("Professional Template")
                .description("A professional resume template")
                .htmlLayout("<html>...</html>")
                .cssStyles("body { font-family: Arial; }")
                .category("PROFESSIONAL")
                .isPremium(false)
                .isActive(true)
                .build();

        mockResponseDTO = TemplateResponseDTO.builder()
                .templateId(1)
                .name("Professional Template")
                .description("A professional resume template")
                .htmlLayout("<html>...</html>")
                .cssStyles("body { font-family: Arial; }")
                .category("PROFESSIONAL")
                .isPremium(false)
                .isActive(true)
                .usageCount(10)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void testCreateTemplate_Success() {
        when(templateMapper.toEntity(mockRequestDTO)).thenReturn(mockTemplate);
        when(templateRepository.save(any(ResumeTemplate.class))).thenReturn(mockTemplate);
        when(templateMapper.toDTO(mockTemplate)).thenReturn(mockResponseDTO);

        TemplateResponseDTO result = templateService.createTemplate(mockRequestDTO);

        assertNotNull(result);
        assertEquals(1, result.getTemplateId());
        assertEquals("Professional Template", result.getName());
        verify(templateRepository, times(1)).save(any(ResumeTemplate.class));
    }

    @Test
    void testGetTemplateById_Success() {
        when(templateRepository.findById(1)).thenReturn(Optional.of(mockTemplate));
        when(templateMapper.toDTO(mockTemplate)).thenReturn(mockResponseDTO);

        TemplateResponseDTO result = templateService.getTemplateById(1);

        assertNotNull(result);
        assertEquals(1, result.getTemplateId());
        assertEquals("Professional Template", result.getName());
        verify(templateRepository, times(1)).findById(1);
    }

    @Test
    void testGetTemplateById_ResourceNotFoundException() {
        when(templateRepository.findById(999)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> templateService.getTemplateById(999));

        assertEquals("Template not found with ID: 999", exception.getMessage());
        verify(templateRepository, times(1)).findById(999);
    }

    @Test
    void testGetAllTemplates_Success() {
        List<ResumeTemplate> templates = List.of(mockTemplate);
        List<TemplateResponseDTO> responseDTOs = List.of(mockResponseDTO);

        when(templateRepository.findAll()).thenReturn(templates);
        when(templateMapper.toDTO(mockTemplate)).thenReturn(mockResponseDTO);

        List<TemplateResponseDTO> result = templateService.getAllTemplates();

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(templateRepository, times(1)).findAll();
    }

    @Test
    void testGetFreeTemplates_Success() {
        List<ResumeTemplate> templates = List.of(mockTemplate);
        when(templateRepository.findByIsPremium(false)).thenReturn(templates);
        when(templateMapper.toDTO(mockTemplate)).thenReturn(mockResponseDTO);

        List<TemplateResponseDTO> result = templateService.getFreeTemplates();

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(templateRepository, times(1)).findByIsPremium(false);
    }

    @Test
    void testGetPremiumTemplates_Success() {
        List<ResumeTemplate> templates = List.of(mockTemplate);
        when(templateRepository.findByIsPremium(true)).thenReturn(templates);
        when(templateMapper.toDTO(mockTemplate)).thenReturn(mockResponseDTO);

        List<TemplateResponseDTO> result = templateService.getPremiumTemplates();

        assertNotNull(result);
        verify(templateRepository, times(1)).findByIsPremium(true);
    }

    @Test
    void testGetTemplatesByCategory_Success() {
        List<ResumeTemplate> templates = List.of(mockTemplate);
        when(templateRepository.findByCategory(TemplateCategory.PROFESSIONAL)).thenReturn(templates);
        when(templateMapper.toDTO(mockTemplate)).thenReturn(mockResponseDTO);

        List<TemplateResponseDTO> result = templateService.getTemplatesByCategory("PROFESSIONAL");

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(templateRepository, times(1)).findByCategory(TemplateCategory.PROFESSIONAL);
    }

    @Test
    void testGetPopularTemplates_Success() {
        List<ResumeTemplate> templates = List.of(mockTemplate);
        when(templateRepository.findAllByOrderByUsageCountDesc()).thenReturn(templates);
        when(templateMapper.toDTO(mockTemplate)).thenReturn(mockResponseDTO);

        List<TemplateResponseDTO> result = templateService.getPopularTemplates();

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(templateRepository, times(1)).findAllByOrderByUsageCountDesc();
    }

    @Test
    void testUpdateTemplate_Success() {
        when(templateRepository.findById(1)).thenReturn(Optional.of(mockTemplate));
        when(templateRepository.save(any(ResumeTemplate.class))).thenReturn(mockTemplate);
        when(templateMapper.toDTO(mockTemplate)).thenReturn(mockResponseDTO);

        TemplateResponseDTO result = templateService.updateTemplate(1, mockRequestDTO);

        assertNotNull(result);
        verify(templateRepository, times(1)).findById(1);
        verify(templateRepository, times(1)).save(any(ResumeTemplate.class));
    }

    @Test
    void testUpdateTemplate_ResourceNotFoundException() {
        when(templateRepository.findById(999)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> templateService.updateTemplate(999, mockRequestDTO));

        assertEquals("Template not found with ID: 999", exception.getMessage());
    }

    @Test
    void testDeactivateTemplate_Success() {
        when(templateRepository.findById(1)).thenReturn(Optional.of(mockTemplate));
        when(templateRepository.save(any(ResumeTemplate.class))).thenReturn(mockTemplate);

        templateService.deactivateTemplate(1);

        verify(templateRepository, times(1)).findById(1);
        verify(templateRepository, times(1)).save(any(ResumeTemplate.class));
        assertFalse(mockTemplate.isActive());
    }

    @Test
    void testDeactivateTemplate_ResourceNotFoundException() {
        when(templateRepository.findById(999)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> templateService.deactivateTemplate(999));

        assertEquals("Template not found with ID: 999", exception.getMessage());
    }

    @Test
    void testIncrementUsageCount_Success() {
        int initialUsageCount = mockTemplate.getUsageCount();
        
        when(templateRepository.findById(1)).thenReturn(Optional.of(mockTemplate));
        when(templateRepository.save(any(ResumeTemplate.class))).thenReturn(mockTemplate);

        templateService.incrementUsageCount(1);

        verify(templateRepository, times(1)).findById(1);
        verify(templateRepository, times(1)).save(any(ResumeTemplate.class));
        assertEquals(initialUsageCount + 1, mockTemplate.getUsageCount());
    }

    @Test
    void testIncrementUsageCount_ResourceNotFoundException() {
        when(templateRepository.findById(999)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> templateService.incrementUsageCount(999));

        assertEquals("Template not found with ID: 999", exception.getMessage());
    }

    @Test
    void testIncrementUsageCount_VerifyCorrectIncrement() {
        ResumeTemplate templateWithZeroUsage = ResumeTemplate.builder()
                .templateId(2)
                .name("New Template")
                .usageCount(0)
                .build();

        when(templateRepository.findById(2)).thenReturn(Optional.of(templateWithZeroUsage));
        when(templateRepository.save(any(ResumeTemplate.class))).thenReturn(templateWithZeroUsage);

        templateService.incrementUsageCount(2);

        assertEquals(1, templateWithZeroUsage.getUsageCount());
        verify(templateRepository, times(1)).save(templateWithZeroUsage);
    }
}

