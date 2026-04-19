//package com.resumeai.resume_service.service.impl;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.Mockito.*;
//
//import java.time.LocalDateTime;
//import java.util.Arrays;
//import java.util.List;
//import java.util.Optional;
//
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import com.resumeai.resume_service.dto.ResumeRequestDTO;
//import com.resumeai.resume_service.dto.ResumeResponseDTO;
//import com.resumeai.resume_service.entity.Resume;
//import com.resumeai.resume_service.exception.ResourceNotFoundException;
//import com.resumeai.resume_service.repository.ResumeRepository;
//
//@ExtendWith(MockitoExtension.class)
//@DisplayName("ResumeServiceImpl Tests")
//class ResumeServiceImplTest {
//
//    @Mock
//    private ResumeRepository resumeRepository;
//
//    @InjectMocks
//    private ResumeServiceImpl resumeService;
//
//    private Resume testResume;
//    private ResumeRequestDTO testResumeRequestDTO;
//
//    @BeforeEach
//    void setUp() {
//        testResume = Resume.builder()
//                .id(1L)
//                .userId(1L)
//                .title("My Test Resume")
//                .content("Test content")
//                .isPublic(false)
//                .viewCount(5)
//                .status("DRAFT")
//                .description("Test description")
//                .createdAt(LocalDateTime.now())
//                .updatedAt(LocalDateTime.now())
//                .build();
//
//        testResumeRequestDTO = ResumeRequestDTO.builder()
//                .userId(1L)
//                .title("My Test Resume")
//                .content("Test content")
//                .isPublic(false)
//                .status("DRAFT")
//                .description("Test description")
//                .build();
//    }
//
//    @Test
//    @DisplayName("Should successfully create a resume")
//    void testCreateResume() {
//        // Arrange
//        Resume savedResume = testResume;
//        when(resumeRepository.save(any(Resume.class))).thenReturn(savedResume);
//
//        // Act
//        ResumeResponseDTO result = resumeService.createResume(testResumeRequestDTO);
//
//        // Assert
//        assertNotNull(result);
//        assertEquals(1L, result.getId());
//        assertEquals("My Test Resume", result.getTitle());
//        assertEquals(1L, result.getUserId());
//        verify(resumeRepository, times(1)).save(any(Resume.class));
//    }
//
//    @Test
//    @DisplayName("Should successfully get resume by ID")
//    void testGetResumeById() {
//        // Arrange
//        when(resumeRepository.findById(1L)).thenReturn(Optional.of(testResume));
//
//        // Act
//        ResumeResponseDTO result = resumeService.getResumeById(1L);
//
//        // Assert
//        assertNotNull(result);
//        assertEquals(1L, result.getId());
//        assertEquals("My Test Resume", result.getTitle());
//        verify(resumeRepository, times(1)).findById(1L);
//    }
//
//    @Test
//    @DisplayName("Should throw ResourceNotFoundException when resume not found")
//    void testGetResumeByIdNotFound() {
//        // Arrange
//        when(resumeRepository.findById(999L)).thenReturn(Optional.empty());
//
//        // Act & Assert
//        assertThrows(ResourceNotFoundException.class, () -> {
//            resumeService.getResumeById(999L);
//        });
//        verify(resumeRepository, times(1)).findById(999L);
//    }
//
//    @Test
//    @DisplayName("Should get all resumes by user ID")
//    void testGetResumesByUserId() {
//        // Arrange
//        Resume resume2 = Resume.builder()
//                .id(2L)
//                .userId(1L)
//                .title("Resume 2")
//                .content("Test content")
//                .isPublic(false)
//                .viewCount(5)
//                .status("DRAFT")
//                .description("Test description")
//                .createdAt(LocalDateTime.now())
//                .updatedAt(LocalDateTime.now())
//                .build();
//        List<Resume> resumeList = Arrays.asList(testResume, resume2);
//        when(resumeRepository.findByUserId(1L)).thenReturn(resumeList);
//
//        // Act
//        List<ResumeResponseDTO> result = resumeService.getResumesByUserId(1L);
//
//        // Assert
//        assertNotNull(result);
//        assertEquals(2, result.size());
//        assertEquals("My Test Resume", result.get(0).getTitle());
//        assertEquals("Resume 2", result.get(1).getTitle());
//        verify(resumeRepository, times(1)).findByUserId(1L);
//    }
//
//    @Test
//    @DisplayName("Should get all public resumes")
//    void testGetPublicResumes() {
//        // Arrange
//        Resume publicResume = Resume.builder()
//                .id(1L)
//                .userId(1L)
//                .title("My Test Resume")
//                .content("Test content")
//                .isPublic(true)
//                .viewCount(5)
//                .status("DRAFT")
//                .description("Test description")
//                .createdAt(LocalDateTime.now())
//                .updatedAt(LocalDateTime.now())
//                .build();
//        List<Resume> publicResumes = Arrays.asList(publicResume);
//        when(resumeRepository.findByIsPublicTrue()).thenReturn(publicResumes);
//
//        // Act
//        List<ResumeResponseDTO> result = resumeService.getPublicResumes();
//
//        // Assert
//        assertNotNull(result);
//        assertEquals(1, result.size());
//        assertTrue(result.get(0).getIsPublic());
//        verify(resumeRepository, times(1)).findByIsPublicTrue();
//    }
//
//    @Test
//    @DisplayName("Should successfully update a resume")
//    void testUpdateResume() {
//        // Arrange
//        ResumeRequestDTO updateDTO = ResumeRequestDTO.builder()
//                .userId(1L)
//                .title("Updated Title")
//                .content("Updated content")
//                .isPublic(true)
//                .status("FINAL")
//                .build();
//
//        when(resumeRepository.findById(1L)).thenReturn(Optional.of(testResume));
//        when(resumeRepository.save(any(Resume.class))).thenReturn(testResume);
//
//        // Act
//        ResumeResponseDTO result = resumeService.updateResume(1L, updateDTO);
//
//        // Assert
//        assertNotNull(result);
//        verify(resumeRepository, times(1)).findById(1L);
//        verify(resumeRepository, times(1)).save(any(Resume.class));
//    }
//
//    @Test
//    @DisplayName("Should throw exception when updating non-existent resume")
//    void testUpdateResumeNotFound() {
//        // Arrange
//        when(resumeRepository.findById(999L)).thenReturn(Optional.empty());
//
//        // Act & Assert
//        assertThrows(ResourceNotFoundException.class, () -> {
//            resumeService.updateResume(999L, testResumeRequestDTO);
//        });
//        verify(resumeRepository, times(1)).findById(999L);
//    }
//
//    @Test
//    @DisplayName("Should successfully duplicate a resume with 'Copy of' prefix")
//    void testDuplicateResume() {
//        // Arrange
//        Resume duplicatedResume = Resume.builder()
//                .id(2L)
//                .userId(1L)
//                .title("Copy of " + testResume.getTitle())
//                .content("Test content")
//                .isPublic(false)
//                .viewCount(0)
//                .status("DRAFT")
//                .description("Test description")
//                .createdAt(LocalDateTime.now())
//                .updatedAt(LocalDateTime.now())
//                .build();
//
//        when(resumeRepository.findById(1L)).thenReturn(Optional.of(testResume));
//        when(resumeRepository.save(any(Resume.class))).thenReturn(duplicatedResume);
//
//        // Act
//        ResumeResponseDTO result = resumeService.duplicateResume(1L);
//
//        // Assert
//        assertNotNull(result);
//        assertEquals(2L, result.getId());
//        assertTrue(result.getTitle().startsWith("Copy of"));
//        assertFalse(result.getIsPublic());
//        assertEquals(0, result.getViewCount());
//        verify(resumeRepository, times(1)).findById(1L);
//        verify(resumeRepository, times(1)).save(any(Resume.class));
//    }
//
//    @Test
//    @DisplayName("Should throw exception when duplicating non-existent resume")
//    void testDuplicateResumeNotFound() {
//        // Arrange
//        when(resumeRepository.findById(999L)).thenReturn(Optional.empty());
//
//        // Act & Assert
//        assertThrows(ResourceNotFoundException.class, () -> {
//            resumeService.duplicateResume(999L);
//        });
//        verify(resumeRepository, times(1)).findById(999L);
//    }
//
//    @Test
//    @DisplayName("Should successfully publish a resume")
//    void testPublishResume() {
//        // Arrange
//        Resume publishedResume = Resume.builder()
//                .id(1L)
//                .userId(1L)
//                .title("My Test Resume")
//                .content("Test content")
//                .isPublic(true)
//                .viewCount(5)
//                .status("DRAFT")
//                .description("Test description")
//                .createdAt(LocalDateTime.now())
//                .updatedAt(LocalDateTime.now())
//                .build();
//        when(resumeRepository.findById(1L)).thenReturn(Optional.of(testResume));
//        when(resumeRepository.save(any(Resume.class))).thenReturn(publishedResume);
//
//        // Act
//        ResumeResponseDTO result = resumeService.publishResume(1L, true);
//
//        // Assert
//        assertNotNull(result);
//        assertTrue(result.getIsPublic());
//        verify(resumeRepository, times(1)).findById(1L);
//        verify(resumeRepository, times(1)).save(any(Resume.class));
//    }
//
//    @Test
//    @DisplayName("Should successfully unpublish a resume")
//    void testUnpublishResume() {
//        // Arrange
//        Resume unpublishedResume = Resume.builder()
//                .id(1L)
//                .userId(1L)
//                .title("My Test Resume")
//                .content("Test content")
//                .isPublic(false)
//                .viewCount(5)
//                .status("DRAFT")
//                .description("Test description")
//                .createdAt(LocalDateTime.now())
//                .updatedAt(LocalDateTime.now())
//                .build();
//        when(resumeRepository.findById(1L)).thenReturn(Optional.of(testResume));
//        when(resumeRepository.save(any(Resume.class))).thenReturn(unpublishedResume);
//
//        // Act
//        ResumeResponseDTO result = resumeService.publishResume(1L, false);
//
//        // Assert
//        assertNotNull(result);
//        assertFalse(result.getIsPublic());
//        verify(resumeRepository, times(1)).findById(1L);
//        verify(resumeRepository, times(1)).save(any(Resume.class));
//    }
//
//    @Test
//    @DisplayName("Should throw exception when publishing non-existent resume")
//    void testPublishResumeNotFound() {
//        // Arrange
//        when(resumeRepository.findById(999L)).thenReturn(Optional.empty());
//
//        // Act & Assert
//        assertThrows(ResourceNotFoundException.class, () -> {
//            resumeService.publishResume(999L, true);
//        });
//        verify(resumeRepository, times(1)).findById(999L);
//    }
//
//    @Test
//    @DisplayName("Should successfully increment view count")
//    void testIncrementViewCount() {
//        // Arrange
//        when(resumeRepository.findById(1L)).thenReturn(Optional.of(testResume));
//        when(resumeRepository.save(any(Resume.class))).thenReturn(testResume);
//
//        // Act
//        resumeService.incrementViewCount(1L);
//
//        // Assert
//        verify(resumeRepository, times(1)).findById(1L);
//        verify(resumeRepository, times(1)).save(any(Resume.class));
//    }
//
//    @Test
//    @DisplayName("Should throw exception when incrementing view count for non-existent resume")
//    void testIncrementViewCountNotFound() {
//        // Arrange
//        when(resumeRepository.findById(999L)).thenReturn(Optional.empty());
//
//        // Act & Assert
//        assertThrows(ResourceNotFoundException.class, () -> {
//            resumeService.incrementViewCount(999L);
//        });
//        verify(resumeRepository, times(1)).findById(999L);
//    }
//
//    @Test
//    @DisplayName("Should get resumes by user ID and public status")
//    void testGetResumesByUserIdAndPublic() {
//        // Arrange
//        List<Resume> resumeList = Arrays.asList(testResume);
//        when(resumeRepository.findByUserIdAndIsPublic(1L, false)).thenReturn(resumeList);
//
//        // Act
//        List<ResumeResponseDTO> result = resumeService.getResumesByUserIdAndPublic(1L, false);
//
//        // Assert
//        assertNotNull(result);
//        assertEquals(1, result.size());
//        assertFalse(result.get(0).getIsPublic());
//        verify(resumeRepository, times(1)).findByUserIdAndIsPublic(1L, false);
//    }
//
//    @Test
//    @DisplayName("Should successfully count resumes by user ID")
//    void testCountResumesByUserId() {
//        // Arrange
//        when(resumeRepository.countByUserId(1L)).thenReturn(3);
//
//        // Act
//        Integer count = resumeService.countResumesByUserId(1L);
//
//        // Assert
//        assertNotNull(count);
//        assertEquals(3, count);
//        verify(resumeRepository, times(1)).countByUserId(1L);
//    }
//}
//
//
//
//
//
//
