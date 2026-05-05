package com.resumeai.aiservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeai.aiservice.dto.AiRequestDTO;
import com.resumeai.aiservice.dto.AtsReportDTO;
import com.resumeai.aiservice.dto.QuotaDTO;
import com.resumeai.aiservice.service.AiService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AiController.class)
@AutoConfigureMockMvc(addFilters = false)
class AiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AiService aiService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testWelcome() throws Exception {
        mockMvc.perform(get("/api/v1/ai/welcome"))
                .andExpect(status().isOk())
                .andExpect(content().string("AI Service is running!"));
    }

    @Test
    void testGenerateSummary() throws Exception {
        AiController.SummaryRequest request = new AiController.SummaryRequest();
        request.setUserId(1L);
        request.setResumeId(10L);
        request.setResumeContent("Software Engineer with 5 years experience.");

        when(aiService.generateSummary(1L, 10L, request.getResumeContent())).thenReturn(new AiRequestDTO());

        mockMvc.perform(post("/api/v1/ai/generate-summary")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void testImproveContent_Summary() throws Exception {
        AiController.ImproveContentRequest request = new AiController.ImproveContentRequest();
        request.setUserId(1L);
        request.setResumeId(10L);
        request.setText("Good at coding.");
        request.setType("summary");

        AiRequestDTO mockResponse = new AiRequestDTO();
        mockResponse.setAiResponse("Highly skilled software developer.");
        mockResponse.setModel("Gemini");

        when(aiService.generateSummary(eq(1L), eq(10L), anyString())).thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/ai/improve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.improvedText").value("Highly skilled software developer."));
    }

    @Test
    void testGetUsage() throws Exception {
        QuotaDTO quotaDTO = QuotaDTO.builder().usedQuota(20).remainingQuota(80).totalMonthlyQuota(100).build();
        when(aiService.getQuotaInfo(1L)).thenReturn(quotaDTO);

        mockMvc.perform(get("/api/v1/ai/usage/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usage").value(20));
    }

    @Test
    void testGenerateBullets() throws Exception {
        AiController.BulletsRequest request = new AiController.BulletsRequest();
        request.setUserId(1L);
        request.setResumeId(10L);
        request.setResumeContent("Did some project");

        when(aiService.generateBullets(anyLong(), anyLong(), anyString())).thenReturn(new AiRequestDTO());

        mockMvc.perform(post("/api/v1/ai/generate-bullets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void testGenerateCoverLetter() throws Exception {
        AiController.CoverLetterRequest request = new AiController.CoverLetterRequest();
        request.setUserId(1L);
        request.setResumeId(10L);
        request.setResumeContent("Resume Data");
        request.setJobDescription("Job Data");

        when(aiService.generateCoverLetter(anyLong(), anyLong(), anyString(), anyString())).thenReturn(new AiRequestDTO());

        mockMvc.perform(post("/api/v1/ai/generate-cover-letter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void testCheckAtsCompatibility() throws Exception {
        AiController.AtsRequest request = new AiController.AtsRequest();
        request.setUserId(1L);
        request.setResumeContent("Resume text");
        request.setJobDescription("JD text");

        when(aiService.checkAtsCompatibility(anyLong(), any(), anyString(), anyString())).thenReturn(AtsReportDTO.builder().build());

        mockMvc.perform(post("/api/v1/ai/check-ats")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void testCheckAtsCompatibilityFromUpload() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "resume.txt", "text/plain", "Resume content file".getBytes());

        when(aiService.checkAtsCompatibility(eq(1L), isNull(), anyString(), anyString())).thenReturn(AtsReportDTO.builder().build());

        mockMvc.perform(multipart("/api/v1/ai/check-ats/upload")
                        .file(file)
                        .header("X-User-Id", "1")
                        .param("jobDescription", "Wanted: Java Dev"))
                .andExpect(status().isOk());
    }

    @Test
    void testTailorResume() throws Exception {
        AiController.TailorRequest request = new AiController.TailorRequest();
        request.setUserId(1L);
        request.setResumeId(10L);
        request.setResumeJson("{}");
        request.setJobDescription("Job Data");

        when(aiService.tailorResumeForJob(anyLong(), anyLong(), anyString(), anyString())).thenReturn(new AiRequestDTO());

        mockMvc.perform(post("/api/v1/ai/tailor-resume")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void testGetHistory() throws Exception {
        when(aiService.getRequestHistory(1L)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/ai/history/1"))
                .andExpect(status().isOk());
    }

    @Test
    void testGetQuota() throws Exception {
        when(aiService.getQuotaInfo(1L)).thenReturn(QuotaDTO.builder().build());

        mockMvc.perform(get("/api/v1/ai/quota/1"))
                .andExpect(status().isOk());
    }
}