package com.resumeai.section.service.impl;

import com.resumeai.resume_section_service.dto.ResumeSectionRequestDTO;
import com.resumeai.resume_section_service.dto.ResumeSectionResponseDTO;
import com.resumeai.resume_section_service.entity.ResumeSection;
import com.resumeai.resume_section_service.exception.ResourceNotFoundException;
import com.resumeai.resume_section_service.mapper.SectionMapper;
import com.resumeai.resume_section_service.repository.SectionRepository;
import com.resumeai.resume_section_service.service.impl.SectionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SectionService Unit Tests")
class SectionServiceImplTest {

    @Mock
    private SectionRepository sectionRepository;

    @Mock
    private SectionMapper sectionMapper;

    @InjectMocks
    private SectionServiceImpl sectionService;

    private ResumeSectionRequestDTO requestDTO;
    private ResumeSection section;
    private ResumeSectionResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        requestDTO = ResumeSectionRequestDTO.builder()
                .resumeId(1L)
                .sectionType("SUMMARY")
                .title("Professional Summary")
                .content("I am a software engineer with 5 years of experience")
                .displayOrder(1)
                .isVisible(true)
                .aiGenerated(false)
                .build();

        section = ResumeSection.builder()
                .sectionId(1L)
                .resumeId(1L)
                .sectionType("SUMMARY")
                .title("Professional Summary")
                .content("I am a software engineer with 5 years of experience")
                .displayOrder(1)
                .isVisible(true)
                .aiGenerated(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        responseDTO = ResumeSectionResponseDTO.builder()
                .sectionId(1L)
                .resumeId(1L)
                .sectionType("SUMMARY")
                .title("Professional Summary")
                .content("I am a software engineer with 5 years of experience")
                .displayOrder(1)
                .isVisible(true)
                .aiGenerated(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should create a new section successfully")
    void testAddSection_Success() {
        // Arrange
        when(sectionMapper.toEntity(requestDTO)).thenReturn(section);
        when(sectionRepository.save(any(ResumeSection.class))).thenReturn(section);
        when(sectionMapper.toResponseDTO(section)).thenReturn(responseDTO);

        // Act
        ResumeSectionResponseDTO result = sectionService.addSection(requestDTO);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getSectionId());
        assertEquals("SUMMARY", result.getSectionType());
        assertEquals("Professional Summary", result.getTitle());
        verify(sectionRepository, times(1)).save(any(ResumeSection.class));
    }

    @Test
    @DisplayName("Should retrieve section by ID successfully")
    void testGetSectionById_Success() {
        // Arrange
        when(sectionRepository.findBySectionId(1L)).thenReturn(Optional.of(section));
        when(sectionMapper.toResponseDTO(section)).thenReturn(responseDTO);

        // Act
        ResumeSectionResponseDTO result = sectionService.getSectionById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getSectionId());
        assertEquals("Professional Summary", result.getTitle());
        verify(sectionRepository, times(1)).findBySectionId(1L);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when section not found by ID")
    void testGetSectionById_NotFound() {
        // Arrange
        when(sectionRepository.findBySectionId(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            sectionService.getSectionById(999L);
        });
        verify(sectionRepository, times(1)).findBySectionId(999L);
    }

    @Test
    @DisplayName("Should retrieve all sections by resume ID")
    void testGetSectionsByResume_Success() {
        // Arrange
        List<ResumeSection> sections = new ArrayList<>();
        sections.add(section);

        List<ResumeSectionResponseDTO> responseDTOs = new ArrayList<>();
        responseDTOs.add(responseDTO);

        when(sectionRepository.findByResumeId(1L)).thenReturn(sections);
        when(sectionMapper.toResponseDTOList(sections)).thenReturn(responseDTOs);

        // Act
        List<ResumeSectionResponseDTO> result = sectionService.getSectionsByResume(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Professional Summary", result.get(0).getTitle());
        verify(sectionRepository, times(1)).findByResumeId(1L);
    }

    @Test
    @DisplayName("Should retrieve empty list when no sections found for resume")
    void testGetSectionsByResume_Empty() {
        // Arrange
        when(sectionRepository.findByResumeId(2L)).thenReturn(new ArrayList<>());
        when(sectionMapper.toResponseDTOList(new ArrayList<>())).thenReturn(new ArrayList<>());

        // Act
        List<ResumeSectionResponseDTO> result = sectionService.getSectionsByResume(2L);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(sectionRepository, times(1)).findByResumeId(2L);
    }

    @Test
    @DisplayName("Should retrieve sections by resume ID ordered by display order")
    void testGetSectionsByResumeOrderByDisplayOrder_Success() {
        // Arrange
        List<ResumeSection> sections = new ArrayList<>();
        sections.add(section);

        List<ResumeSectionResponseDTO> responseDTOs = new ArrayList<>();
        responseDTOs.add(responseDTO);

        when(sectionRepository.findByResumeIdOrderByDisplayOrder(1L)).thenReturn(sections);
        when(sectionMapper.toResponseDTOList(sections)).thenReturn(responseDTOs);

        // Act
        List<ResumeSectionResponseDTO> result = sectionService.getSectionsByResumeOrderByDisplayOrder(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(sectionRepository, times(1)).findByResumeIdOrderByDisplayOrder(1L);
    }

    @Test
    @DisplayName("Should retrieve sections by type")
    void testGetSectionsByType_Success() {
        // Arrange
        List<ResumeSection> sections = new ArrayList<>();
        sections.add(section);

        List<ResumeSectionResponseDTO> responseDTOs = new ArrayList<>();
        responseDTOs.add(responseDTO);

        when(sectionRepository.findByResumeIdAndSectionType(1L, "SUMMARY")).thenReturn(sections);
        when(sectionMapper.toResponseDTOList(sections)).thenReturn(responseDTOs);

        // Act
        List<ResumeSectionResponseDTO> result = sectionService.getSectionsByType(1L, "SUMMARY");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("SUMMARY", result.get(0).getSectionType());
        verify(sectionRepository, times(1)).findByResumeIdAndSectionType(1L, "SUMMARY");
    }

    @Test
    @DisplayName("Should update section successfully")
    void testUpdateSection_Success() {
        // Arrange
        when(sectionRepository.findBySectionId(1L)).thenReturn(Optional.of(section));
        when(sectionRepository.save(any(ResumeSection.class))).thenReturn(section);
        when(sectionMapper.toResponseDTO(section)).thenReturn(responseDTO);

        // Act
        ResumeSectionResponseDTO result = sectionService.updateSection(1L, requestDTO);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getSectionId());
        verify(sectionRepository, times(1)).findBySectionId(1L);
        verify(sectionRepository, times(1)).save(any(ResumeSection.class));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when updating non-existent section")
    void testUpdateSection_NotFound() {
        // Arrange
        when(sectionRepository.findBySectionId(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            sectionService.updateSection(999L, requestDTO);
        });
        verify(sectionRepository, times(1)).findBySectionId(999L);
        verify(sectionRepository, never()).save(any(ResumeSection.class));
    }

    @Test
    @DisplayName("Should delete section successfully")
    void testDeleteSection_Success() {
        // Arrange
        when(sectionRepository.findBySectionId(1L)).thenReturn(Optional.of(section));
        doNothing().when(sectionRepository).delete(any(ResumeSection.class));

        // Act
        sectionService.deleteSection(1L);

        // Assert
        verify(sectionRepository, times(1)).findBySectionId(1L);
        verify(sectionRepository, times(1)).delete(any(ResumeSection.class));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when deleting non-existent section")
    void testDeleteSection_NotFound() {
        // Arrange
        when(sectionRepository.findBySectionId(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            sectionService.deleteSection(999L);
        });
        verify(sectionRepository, never()).delete(any(ResumeSection.class));
    }

    @Test
    @DisplayName("Should delete all sections for a resume")
    void testDeleteAllSectionsByResume_Success() {
        // Arrange
        doNothing().when(sectionRepository).deleteByResumeId(1L);

        // Act
        sectionService.deleteAllSectionsByResume(1L);

        // Assert
        verify(sectionRepository, times(1)).deleteByResumeId(1L);
    }

    @Test
    @DisplayName("Should toggle section visibility successfully")
    void testToggleVisibility_Success() {
        // Arrange
        ResumeSection toggledSection = ResumeSection.builder()
                .sectionId(1L)
                .resumeId(1L)
                .sectionType("SUMMARY")
                .title("Professional Summary")
                .content("I am a software engineer with 5 years of experience")
                .displayOrder(1)
                .isVisible(false)  // toggled
                .aiGenerated(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        ResumeSectionResponseDTO toggledResponseDTO = ResumeSectionResponseDTO.builder()
                .sectionId(1L)
                .resumeId(1L)
                .sectionType("SUMMARY")
                .title("Professional Summary")
                .content("I am a software engineer with 5 years of experience")
                .displayOrder(1)
                .isVisible(false)
                .aiGenerated(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(sectionRepository.findBySectionId(1L)).thenReturn(Optional.of(section));
        when(sectionRepository.save(any(ResumeSection.class))).thenReturn(toggledSection);
        when(sectionMapper.toResponseDTO(toggledSection)).thenReturn(toggledResponseDTO);

        // Act
        ResumeSectionResponseDTO result = sectionService.toggleVisibility(1L);

        // Assert
        assertNotNull(result);
        assertFalse(result.getIsVisible());
        verify(sectionRepository, times(1)).findBySectionId(1L);
        verify(sectionRepository, times(1)).save(any(ResumeSection.class));
    }

    @Test
    @DisplayName("Should count sections by resume ID")
    void testCountSectionsByResume_Success() {
        // Arrange
        when(sectionRepository.countByResumeId(1L)).thenReturn(3L);

        // Act
        Long count = sectionService.countSectionsByResume(1L);

        // Assert
        assertEquals(3L, count);
        verify(sectionRepository, times(1)).countByResumeId(1L);
    }

    @Test
    @DisplayName("Should retrieve AI-generated sections")
    void testGetAiGeneratedSections_Success() {
        // Arrange
        ResumeSection aiSection = ResumeSection.builder()
                .sectionId(2L)
                .resumeId(1L)
                .sectionType("SKILLS")
                .title("Generated Skills")
                .content("Java, Spring Boot, Microservices")
                .displayOrder(2)
                .isVisible(true)
                .aiGenerated(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        ResumeSectionResponseDTO aiResponseDTO = ResumeSectionResponseDTO.builder()
                .sectionId(2L)
                .resumeId(1L)
                .sectionType("SKILLS")
                .title("Generated Skills")
                .content("Java, Spring Boot, Microservices")
                .displayOrder(2)
                .isVisible(true)
                .aiGenerated(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        List<ResumeSection> aiSections = List.of(aiSection);
        List<ResumeSectionResponseDTO> aiResponseDTOs = List.of(aiResponseDTO);

        when(sectionRepository.findByResumeIdAndAiGenerated(1L, true)).thenReturn(aiSections);
        when(sectionMapper.toResponseDTOList(aiSections)).thenReturn(aiResponseDTOs);

        // Act
        List<ResumeSectionResponseDTO> result = sectionService.getAiGeneratedSections(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.get(0).getAiGenerated());
        verify(sectionRepository, times(1)).findByResumeIdAndAiGenerated(1L, true);
    }
}
