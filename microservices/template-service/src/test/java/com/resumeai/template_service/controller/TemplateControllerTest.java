package com.resumeai.template_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeai.template_service.dto.TemplateRequestDTO;
import com.resumeai.template_service.dto.TemplateResponseDTO;
import com.resumeai.template_service.service.TemplateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@AutoConfigureMockMvc
class TemplateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TemplateService templateService;

    @Autowired
    private ObjectMapper objectMapper;

    private TemplateRequestDTO requestDTO;
    private TemplateResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        requestDTO = TemplateRequestDTO.builder()
                .name("Professional Template")
                .description("A professional resume template")
                .htmlLayout("<html>...</html>")
                .cssStyles("body { font-family: Arial; }")
                .category("PROFESSIONAL")
                .isPremium(false)
                .isActive(true)
                .build();

        responseDTO = TemplateResponseDTO.builder()
                .templateId(1)
                .name("Professional Template")
                .description("A professional resume template")
                .htmlLayout("<html>...</html>")
                .cssStyles("body { font-family: Arial; }")
                .category("PROFESSIONAL")
                .isPremium(false)
                .isActive(true)
                .usageCount(0)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void testCreateTemplate_Success() throws Exception {
        when(templateService.createTemplate(any(TemplateRequestDTO.class)))
                .thenReturn(responseDTO);

        mockMvc.perform(post("/api/v1/templates")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.templateId").value(1))
                .andExpect(jsonPath("$.name").value("Professional Template"));

        verify(templateService, times(1)).createTemplate(any(TemplateRequestDTO.class));
    }

    @Test
    void testGetTemplateById_Success() throws Exception {
        when(templateService.getTemplateById(1)).thenReturn(responseDTO);

        mockMvc.perform(get("/api/v1/templates/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.templateId").value(1))
                .andExpect(jsonPath("$.name").value("Professional Template"));

        verify(templateService, times(1)).getTemplateById(1);
    }

    @Test
    void testGetAllTemplates_Success() throws Exception {
        List<TemplateResponseDTO> templates = List.of(responseDTO);
        when(templateService.getAllTemplates()).thenReturn(templates);

        mockMvc.perform(get("/api/v1/templates")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].templateId").value(1));

        verify(templateService, times(1)).getAllTemplates();
    }

    @Test
    void testGetFreeTemplates_Success() throws Exception {
        List<TemplateResponseDTO> templates = List.of(responseDTO);
        when(templateService.getFreeTemplates()).thenReturn(templates);

        mockMvc.perform(get("/api/v1/templates/free")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        verify(templateService, times(1)).getFreeTemplates();
    }

    @Test
    void testGetPremiumTemplates_Success() throws Exception {
        List<TemplateResponseDTO> templates = List.of(responseDTO);
        when(templateService.getPremiumTemplates()).thenReturn(templates);

        mockMvc.perform(get("/api/v1/templates/premium")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        verify(templateService, times(1)).getPremiumTemplates();
    }

    @Test
    void testUpdateTemplate_Success() throws Exception {
        when(templateService.updateTemplate(eq(1), any(TemplateRequestDTO.class)))
                .thenReturn(responseDTO);

        mockMvc.perform(put("/api/v1/templates/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.templateId").value(1));

        verify(templateService, times(1)).updateTemplate(eq(1), any(TemplateRequestDTO.class));
    }

    @Test
    void testDeactivateTemplate_Success() throws Exception {
        doNothing().when(templateService).deactivateTemplate(1);

        mockMvc.perform(put("/api/v1/templates/1/deactivate")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(templateService, times(1)).deactivateTemplate(1);
    }

    @Test
    void testIncrementUsageCount_Success() throws Exception {
        doNothing().when(templateService).incrementUsageCount(1);

        mockMvc.perform(patch("/api/v1/templates/1/increment-usage")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(templateService, times(1)).incrementUsageCount(1);
    }
}

