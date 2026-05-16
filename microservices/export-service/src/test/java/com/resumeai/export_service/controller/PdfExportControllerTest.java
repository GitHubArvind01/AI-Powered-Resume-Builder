package com.resumeai.export_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeai.export_service.dto.PdfExportRequestDTO;
import com.resumeai.export_service.dto.TemplateExportRequest;
import com.resumeai.export_service.dto.TemplateResumeData;
import com.resumeai.export_service.service.ResumePdfExportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = PdfExportController.class)
@AutoConfigureMockMvc(addFilters = false)
class PdfExportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ResumePdfExportService resumePdfExportService;

    @Test
    void exportResumePdf_ValidRequest_ReturnsPdfStream() throws Exception {
        byte[] mockBytes = "fake-pdf-content-stream".getBytes();
        PdfExportRequestDTO dto = new PdfExportRequestDTO(100L);

        when(resumePdfExportService.exportResumePdf(eq(100L), eq(1L))).thenReturn(mockBytes);

        mockMvc.perform(post("/api/v1/export/pdf")
                        .header("X-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=resume-100.pdf"))
                .andExpect(content().bytes(mockBytes));
    }

    @Test
    void exportTemplatePdf_ValidRequest_NormalizesFilenameAndReturnsPdfStream() throws Exception {
        byte[] mockBytes = "fake-template-pdf-stream".getBytes();

        TemplateExportRequest request = new TemplateExportRequest();
        request.setTemplateId("template-modern");
        request.setTemplateName("My Awesome Executive Profile!!! ");
        request.setResumeData(new TemplateResumeData()); // minimal setup to pass @NotNull check if applicable

        when(resumePdfExportService.exportTemplatePdf(any(TemplateExportRequest.class))).thenReturn(mockBytes);

        mockMvc.perform(post("/api/v1/export/template/pdf")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=resume-template-my-awesome-executive-profile-.pdf"))
                .andExpect(content().bytes(mockBytes));
    }
}