package com.resumeai.export_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.resumeai.export_service.exception.ResourceNotFoundException;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayOutputStream;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResumePdfExportService {

    private final DiscoveryClient discoveryClient;
    private final ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private final RestTemplate restTemplate = new RestTemplate();

    public byte[] exportResumePdf(Long resumeId, Long userId) {
        ResumeSnapshot resume = fetchResume(resumeId);

        if (resume.getUserId() == null || !resume.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Resume not found for the authenticated user: " + resumeId);
        }

        return generatePdf(resume);
    }

    private ResumeSnapshot fetchResume(Long resumeId) {
        List<ServiceInstance> instances = discoveryClient.getInstances("RESUME-SERVICE");
        if (instances.isEmpty()) {
            throw new IllegalStateException("Resume service is unavailable");
        }

        String endpoint = instances.get(0).getUri() + "/api/v1/resumes/" + resumeId;
        log.info("Fetching resume {} from {}", resumeId, endpoint);

        ResponseEntity<ResumeSnapshot> response = restTemplate.getForEntity(endpoint, ResumeSnapshot.class);
        ResumeSnapshot body = response.getBody();
        if (body == null) {
            throw new ResourceNotFoundException("Resume not found: " + resumeId);
        }

        return body;
    }

    private byte[] generatePdf(ResumeSnapshot resume) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(outputStream);
            PdfDocument pdfDocument = new PdfDocument(writer);
            Document document = new Document(pdfDocument);

            document.add(new Paragraph(resume.getTitle() == null ? "Resume Export" : resume.getTitle())
                    .setBold()
                    .setFontSize(18));
            document.add(new Paragraph("Resume ID: " + resume.getId()));
            document.add(new Paragraph("Status: " + safe(resume.getStatus())));
            document.add(new Paragraph(" "));
            document.add(new Paragraph(formatContent(resume.getContent())));

            document.close();
            return outputStream.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to generate PDF export", ex);
        }
    }

    private String formatContent(String content) {
        if (content == null || content.isBlank()) {
            return "No resume content available.";
        }

        try {
            Object parsed = objectMapper.readValue(content, Object.class);
            return objectMapper.writeValueAsString(parsed);
        } catch (Exception ignored) {
            return content;
        }
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "DRAFT" : value;
    }

    @Data
    public static class ResumeSnapshot {
        private Long id;
        private Long userId;
        private String title;
        private String content;
        private String status;
    }
}
