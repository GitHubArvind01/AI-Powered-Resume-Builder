package com.resumeai.export_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeai.export_service.dto.ExportRequestDTO;
import com.resumeai.export_service.dto.ExportResponseDTO;
import com.resumeai.export_service.service.ExportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {ExportController.class, PdfExportController.class})
@AutoConfigureMockMvc(addFilters = false)
class ExportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ExportService exportService;

    @MockBean
    private com.resumeai.export_service.service.ResumePdfExportService resumePdfExportService;

    private ExportRequestDTO validRequest;
    private ExportResponseDTO sampleResponse;

    @BeforeEach
    void setUp() {
        validRequest = ExportRequestDTO.builder()
                .resumeId(12L)
                .templateId(3)
                .customizations("{\"color\":\"#000000\"}")
                .build();

        sampleResponse = ExportResponseDTO.builder()
                .jobId("job-uuid-1122")
                .resumeId(12L)
                .userId(1L)
                .status("PROCESSING")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void exportPdf_ValidPayload_ReturnsResponse() throws Exception {
        sampleResponse.setFormat("PDF");
        when(exportService.initiateExport(any(ExportRequestDTO.class), eq(1L))).thenReturn(sampleResponse);

        mockMvc.perform(post("/api/v1/exports/exportPdf")
                        .header("X-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value("job-uuid-1122"))
                .andExpect(jsonPath("$.format").value("PDF"));
    }

    @Test
    void exportDocx_ValidPayload_ReturnsResponse() throws Exception {
        sampleResponse.setFormat("DOCX");
        when(exportService.initiateExport(any(ExportRequestDTO.class), eq(1L))).thenReturn(sampleResponse);

        mockMvc.perform(post("/api/v1/exports/exportDocx")
                        .header("X-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.format").value("DOCX"));
    }

    @Test
    void exportJson_ValidPayload_ReturnsResponse() throws Exception {
        sampleResponse.setFormat("JSON");
        when(exportService.initiateExport(any(ExportRequestDTO.class), eq(1L))).thenReturn(sampleResponse);

        mockMvc.perform(post("/api/v1/exports/exportJson")
                        .header("X-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.format").value("JSON"));
    }

    @Test
    void getJobStatus_ExistingJob_ReturnsDetails() throws Exception {
        when(exportService.getJobStatus("job-uuid-1122")).thenReturn(sampleResponse);

        mockMvc.perform(get("/api/v1/exports/jobStatus/{jobId}", "job-uuid-1122"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value("job-uuid-1122"));
    }

    @Test
    void getExportsByUser_ExistingHeader_ReturnsList() throws Exception {
        when(exportService.getExportsByUser(1L)).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/v1/exports/byUser")
                        .header("X-User-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].jobId").value("job-uuid-1122"));
    }

    @Test
    void getDownloadUrl_ExistingJob_ReturnsRawUrlString() throws Exception {
        String expectedUrl = "https://s3.amazonaws.com/resumes/12.pdf";
        when(exportService.getDownloadUrl("job-uuid-1122")).thenReturn(expectedUrl);

        mockMvc.perform(get("/api/v1/exports/download/{jobId}", "job-uuid-1122"))
                .andExpect(status().isOk())
                .andExpect(content().string(expectedUrl));
    }

    @Test
    void deleteExport_ExistingJob_Returns24NoContent() throws Exception {
        doNothing().when(exportService).deleteExport("job-uuid-1122");

        mockMvc.perform(delete("/api/v1/exports/{jobId}", "job-uuid-1122"))
                .andExpect(status().isNoContent());
    }

    @Test
    void getStats_ReturnsMapPayload() throws Exception {
        when(exportService.getStats()).thenReturn(Map.of("totalExports", 150L));

        mockMvc.perform(get("/api/v1/exports/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalExports").value(150));
    }
}