package com.resumeai.template_service.service.impl;

import com.resumeai.template_service.dto.TemplateRequestDTO;
import com.resumeai.template_service.dto.TemplateResponseDTO;
import com.resumeai.template_service.entity.ResumeTemplate;
import com.resumeai.template_service.entity.TemplateCategory;
import com.resumeai.template_service.exception.ResourceNotFoundException;
import com.resumeai.template_service.mapper.TemplateMapper;
import com.resumeai.template_service.repository.TemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TemplateServiceImplTest {

    @Mock
    private TemplateRepository templateRepository;

    @Mock
    private TemplateMapper templateMapper;

    @InjectMocks
    private TemplateServiceImpl templateService;

    private TemplateRequestDTO requestDTO;
    private ResumeTemplate mockTemplate;
    private TemplateResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        requestDTO = TemplateRequestDTO.builder()
                .name("Creative Studio")
                .category("CREATIVE")
                .isPremium(true)
                .build();

        mockTemplate = ResumeTemplate.builder()
                .templateId(101)
                .name("Creative Studio")
                .category(TemplateCategory.CREATIVE)
                .isPremium(true)
                .isActive(true)
                .usageCount(10)
                .build();

        responseDTO = TemplateResponseDTO.builder()
                .templateId(101)
                .name("Creative Studio")
                .category("CREATIVE")
                .isPremium(true)
                .isActive(true)
                .usageCount(10)
                .build();
    }

    @Test
    void createTemplate_Success_SavesAndReturnsDto() {
        when(templateMapper.toEntity(requestDTO)).thenReturn(mockTemplate);
        when(templateRepository.save(mockTemplate)).thenReturn(mockTemplate);
        when(templateMapper.toDTO(mockTemplate)).thenReturn(responseDTO);

        TemplateResponseDTO result = templateService.createTemplate(requestDTO);

        assertNotNull(result);
        assertEquals(101, result.getTemplateId());
        verify(templateRepository, times(1)).save(mockTemplate);
    }

    @Test
    void getTemplateById_IdExists_ReturnsMappedDto() {
        when(templateRepository.findById(101)).thenReturn(Optional.of(mockTemplate));
        when(templateMapper.toDTO(mockTemplate)).thenReturn(responseDTO);

        TemplateResponseDTO result = templateService.getTemplateById(101);

        assertNotNull(result);
        assertEquals("Creative Studio", result.getName());
    }

    @Test
    void getTemplateById_IdDoesNotExist_ThrowsResourceNotFoundException() {
        when(templateRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> templateService.getTemplateById(999));
        verify(templateMapper, never()).toDTO(any());
    }

    @Test
    void getAllTemplates_ReturnsList() {
        when(templateRepository.findAll()).thenReturn(List.of(mockTemplate));
        when(templateMapper.toDTO(mockTemplate)).thenReturn(responseDTO);

        List<TemplateResponseDTO> results = templateService.getAllTemplates();

        assertEquals(1, results.size());
        assertEquals(101, results.get(0).getTemplateId());
    }

    @Test
    void getFreeTemplates_CallsFilteredRepository() {
        when(templateRepository.findByIsPremium(false)).thenReturn(List.of(mockTemplate));
        when(templateMapper.toDTO(mockTemplate)).thenReturn(responseDTO);

        List<TemplateResponseDTO> results = templateService.getFreeTemplates();

        assertFalse(results.isEmpty());
        verify(templateRepository, times(1)).findByIsPremium(false);
    }

    @Test
    void getPremiumTemplates_CallsFilteredRepository() {
        when(templateRepository.findByIsPremium(true)).thenReturn(List.of(mockTemplate));
        when(templateMapper.toDTO(mockTemplate)).thenReturn(responseDTO);

        List<TemplateResponseDTO> results = templateService.getPremiumTemplates();

        assertFalse(results.isEmpty());
        verify(templateRepository, times(1)).findByIsPremium(true);
    }

    @Test
    void getTemplatesByCategory_NormalizesStringAndQueriesEnum() {
        // Test case sensitivity handling and replacing hyphens like "executive-management" -> "EXECUTIVE_MANAGEMENT"
        when(templateRepository.findByCategory(TemplateCategory.CREATIVE)).thenReturn(List.of(mockTemplate));
        when(templateMapper.toDTO(mockTemplate)).thenReturn(responseDTO);

        List<TemplateResponseDTO> results = templateService.getTemplatesByCategory("creative");

        assertFalse(results.isEmpty());
        verify(templateRepository, times(1)).findByCategory(TemplateCategory.CREATIVE);
    }

    @Test
    void getTemplatesByCategory_InvalidCategoryString_ThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> {
            templateService.getTemplatesByCategory("NON_EXISTENT_CATEGORY_PROBABLE");
        });
        verifyNoInteractions(templateRepository);
    }

    @Test
    void getPopularTemplates_QueriesDescOrderedRepository() {
        when(templateRepository.findAllByOrderByUsageCountDesc()).thenReturn(List.of(mockTemplate));
        when(templateMapper.toDTO(mockTemplate)).thenReturn(responseDTO);

        List<TemplateResponseDTO> results = templateService.getPopularTemplates();

        assertFalse(results.isEmpty());
        verify(templateRepository, times(1)).findAllByOrderByUsageCountDesc();
    }

    @Test
    void updateTemplate_TemplateExists_MutatesAndSaves() {
        when(templateRepository.findById(101)).thenReturn(Optional.of(mockTemplate));
        doNothing().when(templateMapper).updateEntityFromDTO(requestDTO, mockTemplate);
        when(templateRepository.save(mockTemplate)).thenReturn(mockTemplate);
        when(templateMapper.toDTO(mockTemplate)).thenReturn(responseDTO);

        TemplateResponseDTO result = templateService.updateTemplate(101, requestDTO);

        assertNotNull(result);
        verify(templateMapper, times(1)).updateEntityFromDTO(requestDTO, mockTemplate);
        verify(templateRepository, times(1)).save(mockTemplate);
    }

    @Test
    void deactivateTemplate_TemplateExists_SetsActiveToFalseAndSaves() {
        when(templateRepository.findById(101)).thenReturn(Optional.of(mockTemplate));
        when(templateRepository.save(mockTemplate)).thenReturn(mockTemplate);

        templateService.deactivateTemplate(101);

        assertFalse(mockTemplate.isActive());
        verify(templateRepository, times(1)).save(mockTemplate);
    }

    @Test
    void incrementUsageCount_TemplateExists_IncrementsCountAndSaves() {
        when(templateRepository.findById(101)).thenReturn(Optional.of(mockTemplate));
        when(templateRepository.save(mockTemplate)).thenReturn(mockTemplate);

        templateService.incrementUsageCount(101);

        assertEquals(11, mockTemplate.getUsageCount());
        verify(templateRepository, times(1)).save(mockTemplate);
    }
}