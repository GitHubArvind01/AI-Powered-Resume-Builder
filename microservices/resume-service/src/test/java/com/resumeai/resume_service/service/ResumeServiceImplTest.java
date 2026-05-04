package com.resumeai.resume_service.service;

import com.resumeai.resume_service.dto.ResumeRequestDTO;
import com.resumeai.resume_service.dto.ResumeResponseDTO;
import com.resumeai.resume_service.entity.Resume;
import com.resumeai.resume_service.exception.AccessDeniedException;
import com.resumeai.resume_service.exception.ResourceNotFoundException;
import com.resumeai.resume_service.repository.ResumeRepository;
import com.resumeai.resume_service.service.impl.ResumeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResumeServiceImplTest {

    @Mock
    private ResumeRepository resumeRepository;

    @InjectMocks
    private ResumeServiceImpl resumeService;

    private Resume mockResume;
    private ResumeRequestDTO mockRequestDTO;

    @BeforeEach
    void setUp() {
        mockResume = Resume.builder()
                .id(1L)
                .userId(100L)
                .title("Software Engineer Resume")
                .content("Experience in Java and Spring Boot")
                .isPublic(false)
                .viewCount(0)
                .build();

        mockRequestDTO = new ResumeRequestDTO();
        mockRequestDTO.setTitle("Software Engineer Resume");
        mockRequestDTO.setContent("Experience in Java and Spring Boot");
    }

    @Test
    void testCreateResume_Success() {
        when(resumeRepository.save(any(Resume.class))).thenReturn(mockResume);

        ResumeResponseDTO response = resumeService.createResume(mockRequestDTO, 100L);

        assertNotNull(response);
        assertEquals("Software Engineer Resume", response.getTitle());
        verify(resumeRepository, times(1)).save(any(Resume.class));
    }

    @Test
    void testCreateResume_NullUserIdThrowsException() {
        assertThrows(IllegalArgumentException.class, () ->
                resumeService.createResume(mockRequestDTO, null)
        );
    }

    @Test
    void testGetResumeById_Success() {
        when(resumeRepository.findById(1L)).thenReturn(Optional.of(mockResume));

        ResumeResponseDTO response = resumeService.getResumeById(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
    }

    @Test
    void testGetResumeById_NotFound() {
        when(resumeRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> resumeService.getResumeById(1L));
    }

    @Test
    void testGetResumesByUserId() {
        when(resumeRepository.findByUserId(100L)).thenReturn(Arrays.asList(mockResume));

        List<ResumeResponseDTO> responses = resumeService.getResumesByUserId(100L);

        assertFalse(responses.isEmpty());
        assertEquals(1, responses.size());
    }

    @Test
    void testGetPublicResumes() {
        mockResume.setIsPublic(true);
        when(resumeRepository.findByIsPublicTrue()).thenReturn(Arrays.asList(mockResume));

        List<ResumeResponseDTO> responses = resumeService.getPublicResumes();

        assertFalse(responses.isEmpty());
    }

    @Test
    void testUpdateResume_Success() {
        when(resumeRepository.findById(1L)).thenReturn(Optional.of(mockResume));
        when(resumeRepository.save(any(Resume.class))).thenReturn(mockResume);

        mockRequestDTO.setTitle("Updated Title");

        ResumeResponseDTO response = resumeService.updateResume(1L, mockRequestDTO, 100L);

        assertNotNull(response);
        verify(resumeRepository).save(mockResume);
    }

    @Test
    void testUpdateResume_AccessDenied() {
        when(resumeRepository.findById(1L)).thenReturn(Optional.of(mockResume));

        // The resume belongs to userId 100L, so trying to update it with userId 999L should fail
        assertThrows(AccessDeniedException.class, () ->
                resumeService.updateResume(1L, mockRequestDTO, 999L)
        );
    }

    @Test
    void testDeleteResume_Success() {
        when(resumeRepository.findById(1L)).thenReturn(Optional.of(mockResume));
        doNothing().when(resumeRepository).deleteById(1L);

        resumeService.deleteResume(1L, 100L);

        verify(resumeRepository, times(1)).deleteById(1L);
    }

    @Test
    void testDeleteResume_AccessDenied() {
        when(resumeRepository.findById(1L)).thenReturn(Optional.of(mockResume));

        assertThrows(AccessDeniedException.class, () -> resumeService.deleteResume(1L, 999L));
    }

    @Test
    void testDuplicateResume_Success() {
        when(resumeRepository.findById(1L)).thenReturn(Optional.of(mockResume));

        Resume copiedResume = Resume.builder()
                .id(2L)
                .userId(100L)
                .title("Copy of Software Engineer Resume")
                .isPublic(false)
                .build();

        when(resumeRepository.save(any(Resume.class))).thenReturn(copiedResume);

        ResumeResponseDTO response = resumeService.duplicateResume(1L);

        assertNotNull(response);
        assertEquals("Copy of Software Engineer Resume", response.getTitle());
    }

    @Test
    void testPublishResume() {
        when(resumeRepository.findById(1L)).thenReturn(Optional.of(mockResume));
        when(resumeRepository.save(any(Resume.class))).thenReturn(mockResume);

        ResumeResponseDTO response = resumeService.publishResume(1L, true);

        assertNotNull(response);
        assertTrue(mockResume.getIsPublic());
    }

    @Test
    void testIncrementViewCount() {
        when(resumeRepository.findById(1L)).thenReturn(Optional.of(mockResume));
        when(resumeRepository.save(any(Resume.class))).thenReturn(mockResume);

        resumeService.incrementViewCount(1L);

        assertEquals(1, mockResume.getViewCount());
        verify(resumeRepository).save(mockResume);
    }

    @Test
    void testGetResumesByUserIdAndPublic() {
        when(resumeRepository.findByUserIdAndIsPublic(100L, true)).thenReturn(Arrays.asList(mockResume));

        List<ResumeResponseDTO> responses = resumeService.getResumesByUserIdAndPublic(100L, true);

        assertFalse(responses.isEmpty());
    }

    @Test
    void testCountResumesByUserId() {
        when(resumeRepository.countByUserId(100L)).thenReturn(5);

        Integer count = resumeService.countResumesByUserId(100L);

        assertEquals(5, count);
    }

    @Test
    void testUpdateResume_NotFound() {
        when(resumeRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                resumeService.updateResume(1L, mockRequestDTO, 100L)
        );
    }

    @Test
    void testDeleteResume_NotFound() {
        when(resumeRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                resumeService.deleteResume(1L, 100L)
        );
    }

    @Test
    void testDuplicateResume_NotFound() {
        when(resumeRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                resumeService.duplicateResume(1L)
        );
    }

    @Test
    void testPublishResume_NotFound() {
        when(resumeRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                resumeService.publishResume(1L, true)
        );
    }

    @Test
    void testIncrementViewCount_NotFound() {
        when(resumeRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                resumeService.incrementViewCount(1L)
        );
    }
}