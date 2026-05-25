package com.resumeai.export_service.service;

import com.resumeai.export_service.dto.ExportRequestDTO;
import com.resumeai.export_service.dto.ExportResponseDTO;
import com.resumeai.export_service.entity.ExportJob;
import com.resumeai.export_service.exception.ResourceNotFoundException;
import com.resumeai.export_service.repository.ExportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExportServiceImplTest {

    @Mock
    private ExportRepository exportRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private ExportServiceImpl exportService;

    private ExportRequestDTO requestDTO;
    private ExportJob mockJob;

    @BeforeEach
    void setUp() {
        requestDTO = ExportRequestDTO.builder()
                .resumeId(50L)
                .format("PDF")
                .templateId(1)
                .customizations("{}")
                .build();

        mockJob = new ExportJob();
        mockJob.setJobId("random-job-string");
        mockJob.setResumeId(50L);
        mockJob.setUserId(99L);
        mockJob.setFormat("PDF");
        mockJob.setStatus("PENDING");
        mockJob.setFileUrl("http://storage.com/output.pdf");
    }

    @Test
    void initiateExport_SavesJobAndDispatchesToRabbitMQ() {
        // We bypass the internal static mapper mocking by supplying expected outputs
        when(exportRepository.save(any(ExportJob.class))).thenReturn(mockJob);
        doNothing().when(rabbitTemplate).convertAndSend(eq("export.queue"), eq("random-job-string"));

        ExportResponseDTO result = exportService.initiateExport(requestDTO, 99L);

        assertNotNull(result);
        assertEquals("random-job-string", result.getJobId());
        verify(exportRepository, times(1)).save(any(ExportJob.class));
        verify(rabbitTemplate, times(1)).convertAndSend("export.queue", "random-job-string");
    }

    @Test
    void getJobStatus_IdExists_ReturnsMappedPayload() {
        when(exportRepository.findById("random-job-string")).thenReturn(Optional.of(mockJob));

        ExportResponseDTO result = exportService.getJobStatus("random-job-string");

        assertNotNull(result);
        assertEquals("PENDING", result.getStatus());
    }

    @Test
    void getJobStatus_IdDoesNotExist_ThrowsResourceNotFoundException() {
        when(exportRepository.findById("absent-id")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> exportService.getJobStatus("absent-id"));
    }

    @Test
    void getExportsByUser_QueriesFilteredRepository() {
        when(exportRepository.findByUserId(99L)).thenReturn(List.of(mockJob));

        List<ExportResponseDTO> results = exportService.getExportsByUser(99L);

        assertFalse(results.isEmpty());
        assertEquals(1, results.size());
    }

    @Test
    void getDownloadUrl_IdExists_ReturnsRawUrl() {
        when(exportRepository.findById("random-job-string")).thenReturn(Optional.of(mockJob));

        String url = exportService.getDownloadUrl("random-job-string");

        assertEquals("http://storage.com/output.pdf", url);
    }

    @Test
    void deleteExport_IdExists_ExecutesDeletion() {
        when(exportRepository.existsById("random-job-string")).thenReturn(true);
        doNothing().when(exportRepository).deleteById("random-job-string");

        assertDoesNotThrow(() -> exportService.deleteExport("random-job-string"));
        verify(exportRepository, times(1)).deleteById("random-job-string");
    }

    @Test
    void deleteExport_IdDoesNotExist_ThrowsException() {
        when(exportRepository.existsById("absent-id")).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> exportService.deleteExport("absent-id"));
        verify(exportRepository, never()).deleteById(anyString());
    }

    @Test
    void getStats_ReturnsCountAggregates() {
        when(exportRepository.count()).thenReturn(1050L);

        Map<String, Object> stats = exportService.getStats();

        assertEquals(1050L, stats.get("totalExports"));
    }
}