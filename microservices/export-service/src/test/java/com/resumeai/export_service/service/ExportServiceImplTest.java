package com.resumeai.export_service.service;

import com.amazonaws.services.s3.AmazonS3;
import com.resumeai.export_service.dto.ExportRequestDTO;
import com.resumeai.export_service.dto.ExportResponseDTO;
import com.resumeai.export_service.entity.ExportJob;
import com.resumeai.export_service.exception.ResourceNotFoundException;
import com.resumeai.export_service.repository.ExportRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExportServiceImplTest {

    @Mock
    private ExportRepository exportRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private AmazonS3 amazonS3;

    @InjectMocks
    private ExportServiceImpl exportService;

    @Test
    void testInitiateExport_Success() {
        // Arrange
        ExportRequestDTO request = ExportRequestDTO.builder()
                .resumeId(1L)
                .format("PDF")
                .templateId(1)
                .customizations("{\"color\":\"blue\"}")
                .build();
        Long userId = 1L;
        ExportJob savedJob = ExportJob.builder()
                .jobId("test-job-id")
                .resumeId(1L)
                .userId(1L)
                .format("PDF")
                .status("QUEUED")
                .templateId(1)
                .customizations("{\"color\":\"blue\"}")
                .build();
        when(exportRepository.save(any(ExportJob.class))).thenReturn(savedJob);

        // Act
        ExportResponseDTO response = exportService.initiateExport(request, userId);

        // Assert
        assertNotNull(response);
        assertEquals("PDF", response.getFormat());
        assertEquals("QUEUED", response.getStatus());
        verify(exportRepository, times(1)).save(any(ExportJob.class));
        verify(rabbitTemplate, times(1)).convertAndSend("export.queue", "test-job-id");
    }

    @Test
    void testGetJobStatus_ThrowsResourceNotFoundException() {
        // Arrange
        String jobId = "non-existent-job";
        when(exportRepository.findById(jobId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> exportService.getJobStatus(jobId));
        verify(exportRepository, times(1)).findById(jobId);
    }
}
