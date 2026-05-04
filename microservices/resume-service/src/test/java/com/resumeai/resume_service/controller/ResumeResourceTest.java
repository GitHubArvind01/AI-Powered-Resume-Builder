package com.resumeai.resume_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeai.resume_service.dto.ResumeRequestDTO;
import com.resumeai.resume_service.dto.ResumeResponseDTO;
import com.resumeai.resume_service.repository.ResumeRepository;
import com.resumeai.resume_service.service.ResumeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ResumeResource.class)
@AutoConfigureMockMvc(addFilters = false)
class ResumeResourceTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ResumeService resumeService;

    @MockBean
    private ResumeRepository resumeRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testWelcome() throws Exception {
        mockMvc.perform(get("/api/v1/resumes/welcome"))
                .andExpect(status().isOk())
                .andExpect(content().string("Resume Service is running!"));
    }

    @Test
    void testCreateResume() throws Exception {
        ResumeRequestDTO request = new ResumeRequestDTO();
        request.setTitle("My Resume");
        request.setContent("Developer content");

        ResumeResponseDTO response = new ResumeResponseDTO();
        response.setId(1L);

        when(resumeService.createResume(any(ResumeRequestDTO.class), eq(100L))).thenReturn(response);

        mockMvc.perform(post("/api/v1/resumes")
                        .header("X-User-Id", 100L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void testGetResumeById() throws Exception {
        ResumeResponseDTO response = new ResumeResponseDTO();
        response.setId(1L);

        when(resumeService.getResumeById(1L)).thenReturn(response);
        doNothing().when(resumeService).incrementViewCount(1L);

        mockMvc.perform(get("/api/v1/resumes/1"))
                .andExpect(status().isOk());

        verify(resumeService, times(1)).incrementViewCount(1L);
    }

    @Test
    void testGetResumesByUserId() throws Exception {
        when(resumeService.getResumesByUserId(100L)).thenReturn(Collections.singletonList(new ResumeResponseDTO()));

        mockMvc.perform(get("/api/v1/resumes/user/100"))
                .andExpect(status().isOk());
    }

    @Test
    void testGetPublicResumes() throws Exception {
        when(resumeService.getPublicResumes()).thenReturn(Collections.singletonList(new ResumeResponseDTO()));

        mockMvc.perform(get("/api/v1/resumes/public/all"))
                .andExpect(status().isOk());
    }

    @Test
    void testUpdateResume() throws Exception {
        ResumeRequestDTO request = new ResumeRequestDTO();
        request.setTitle("Updated Title");

        when(resumeService.updateResume(eq(1L), any(ResumeRequestDTO.class), eq(100L))).thenReturn(new ResumeResponseDTO());

        mockMvc.perform(put("/api/v1/resumes/1")
                        .header("X-User-Id", 100L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void testDeleteResume() throws Exception {
        doNothing().when(resumeService).deleteResume(1L, 100L);

        mockMvc.perform(delete("/api/v1/resumes/1")
                        .header("X-User-Id", 100L))
                .andExpect(status().isNoContent());
    }

    @Test
    void testDuplicateResume() throws Exception {
        when(resumeService.duplicateResume(1L)).thenReturn(new ResumeResponseDTO());

        mockMvc.perform(post("/api/v1/resumes/1/duplicate"))
                .andExpect(status().isCreated());
    }

    @Test
    void testPublishResume() throws Exception {
        when(resumeService.publishResume(1L, true)).thenReturn(new ResumeResponseDTO());

        mockMvc.perform(put("/api/v1/resumes/1/publish")
                        .param("isPublic", "true"))
                .andExpect(status().isOk());
    }

    @Test
    void testGetResumesByUserIdAndPublic() throws Exception {
        when(resumeService.getResumesByUserIdAndPublic(100L, true)).thenReturn(Collections.singletonList(new ResumeResponseDTO()));

        mockMvc.perform(get("/api/v1/resumes/user/100/filter")
                        .param("isPublic", "true"))
                .andExpect(status().isOk());
    }

    @Test
    void testCountResumesByUserId() throws Exception {
        when(resumeService.countResumesByUserId(100L)).thenReturn(5);

        mockMvc.perform(get("/api/v1/resumes/user/100/count"))
                .andExpect(status().isOk())
                .andExpect(content().string("5"));
    }

    @Test
    void testResumeExistsForUser() throws Exception {
        when(resumeRepository.existsByIdAndUserId(1L, 100L)).thenReturn(true);

        mockMvc.perform(get("/api/v1/resumes/1/user/100/exists"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }
}