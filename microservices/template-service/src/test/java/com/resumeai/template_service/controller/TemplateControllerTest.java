package com.resumeai.template_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeai.template_service.dto.TemplateRequestDTO;
import com.resumeai.template_service.dto.TemplateResponseDTO;
import com.resumeai.template_service.service.TemplateService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = TemplateController.class)
@AutoConfigureMockMvc(addFilters = false) // Ensures spring security config doesn't intercept test requests
class TemplateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TemplateService templateService;

    private TemplateRequestDTO validRequest;
    private TemplateResponseDTO sampleResponse;

    @BeforeEach
    void setUp() {
        validRequest = TemplateRequestDTO.builder()
                .name("Modern Tech Minimalist")
                .description("Clean single page design for developers")
                .thumbnailUrl("http://cdn.com/thumb.png")
                .htmlLayout("<div>{{name}}</div>")
                .cssStyles("body { color: #333; }")
                .category("TECHNICAL")
                .isPremium(false)
                .isActive(true)
                .build();

        sampleResponse = TemplateResponseDTO.builder()
                .templateId(1)
                .name("Modern Tech Minimalist")
                .description("Clean single page design for developers")
                .thumbnailUrl("http://cdn.com/thumb.png")
                .htmlLayout("<div>{{name}}</div>")
                .cssStyles("body { color: #333; }")
                .category("TECHNICAL")
                .isPremium(false)
                .isActive(true)
                .usageCount(42)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void createTemplate_WithValidBody_Returns201AndPayload() throws Exception {
        when(templateService.createTemplate(any(TemplateRequestDTO.class))).thenReturn(sampleResponse);

        mockMvc.perform(post("/api/v1/templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.templateId").value(1))
                .andExpect(jsonPath("$.name").value("Modern Tech Minimalist"));
    }

    @Test
    void createTemplate_WithInvalidBody_Returns400BadRequest() throws Exception {
        TemplateRequestDTO invalidRequest = TemplateRequestDTO.builder()
                .name("") // Blank name violation
                .htmlLayout("") // Blank layout violation
                .build();

        mockMvc.perform(post("/api/v1/templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getTemplateById_ExistingId_ReturnsTemplate() throws Exception {
        when(templateService.getTemplateById(1)).thenReturn(sampleResponse);

        mockMvc.perform(get("/api/v1/templates/{id}", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.templateId").value(1))
                .andExpect(jsonPath("$.name").value("Modern Tech Minimalist"));
    }

    @Test
    void getAllTemplates_ReturnsCompleteList() throws Exception {
        when(templateService.getAllTemplates()).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/v1/templates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].templateId").value(1))
                .andExpect(jsonPath("$.size()").value(1));
    }

    @Test
    void getFreeTemplates_ReturnsFreeTemplatesOnly() throws Exception {
        when(templateService.getFreeTemplates()).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/v1/templates/free"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].isPremium").value(false));
    }

    @Test
    void getPremiumTemplates_ReturnsPremiumTemplatesOnly() throws Exception {
        sampleResponse.setPremium(true);
        when(templateService.getPremiumTemplates()).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/v1/templates/premium"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].isPremium").value(true));
    }

    @Test
    void getTemplatesByCategory_ValidCategoryString_ReturnsList() throws Exception {
        when(templateService.getTemplatesByCategory("technical")).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/v1/templates/category/{category}", "technical"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].category").value("TECHNICAL"));
    }

    @Test
    void getPopularTemplates_ReturnsOrderedList() throws Exception {
        when(templateService.getPopularTemplates()).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/v1/templates/popular"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].usageCount").value(42));
    }

    @Test
    void updateTemplate_ValidPayload_ReturnsUpdatedPayload() throws Exception {
        when(templateService.updateTemplate(eq(1), any(TemplateRequestDTO.class))).thenReturn(sampleResponse);

        mockMvc.perform(put("/api/v1/templates/{id}", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.templateId").value(1));
    }

    @Test
    void deactivateTemplate_ExistingId_Returns204NoContent() throws Exception {
        // CORRECT VOID METHOD STUBBING: move the method call outside when()
        doNothing().when(templateService).deactivateTemplate(1);

        mockMvc.perform(put("/api/v1/templates/{id}/deactivate", 1))
                .andExpect(status().isNoContent());
    }

    @Test
    void incrementUsageCount_ExistingId_Returns204NoContent() throws Exception {
        // CORRECT VOID METHOD STUBBING: move the method call outside when()
        doNothing().when(templateService).incrementUsageCount(1);

        mockMvc.perform(patch("/api/v1/templates/{id}/increment-usage", 1))
                .andExpect(status().isNoContent());
    }
}