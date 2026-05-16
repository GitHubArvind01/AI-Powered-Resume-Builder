package com.resumeai.export_service.service;

import com.resumeai.export_service.dto.*;
import com.resumeai.export_service.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResumePdfExportServiceTest {

    @Mock
    private DiscoveryClient discoveryClient;

    @InjectMocks
    private ResumePdfExportService resumePdfExportService;

    private TemplateExportRequest templateExportRequest;

    @BeforeEach
    void setUp() {
        templateExportRequest = new TemplateExportRequest();
        templateExportRequest.setTemplateId("template-modern");
        templateExportRequest.setTemplateName("Modern Variant");

        TemplateResumeData resumeData = new TemplateResumeData();
        TemplatePersonalInfo personalInfo = new TemplatePersonalInfo();
        personalInfo.setFullName("Arvind Kumar");
        personalInfo.setHeadline("Java Full Stack Developer");
        personalInfo.setEmail("arvind@test.com");
        personalInfo.setPhone("+91 9999999999");
        personalInfo.setLocation("Bhopal, India");

        resumeData.setPersonalInfo(personalInfo);
        resumeData.setSummary("Dedicated engineer experienced in Spring Boot and Angular microservices.");

        // Setup mock subsections to fully exercise HTML branch generation
        TemplateSectionData expItem = new TemplateSectionData();
        expItem.setTitle("Backend Engineer");
        expItem.setSubtitle("Tech Corp");
        expItem.setDateRange("Jan 2024 - Present");
        expItem.setDescription("Developed scalable architecture workflows.");
        expItem.setBullets(List.of("Boosted processing speed by 40%", "Integrated secure OAuth2 filters"));
        resumeData.setExperience(List.of(expItem));

        resumeData.setSkills(List.of("Java", "Spring Boot", "Angular", "MySQL"));
        resumeData.setCertifications(List.of("AWS Certified Cloud Practitioner"));
        resumeData.setLanguages(List.of("English", "Hindi"));

        templateExportRequest.setResumeData(resumeData);

        TemplateStyleConfig styleConfig = new TemplateStyleConfig();
        styleConfig.setAccentColor("#1d4ed8");
        styleConfig.setVariant("modern");
        templateExportRequest.setStyleConfig(styleConfig);
    }

    @Test
    void exportTemplatePdf_ValidPayload_GeneratesBytesSuccessfully() {
        byte[] pdfBytes = resumePdfExportService.exportTemplatePdf(templateExportRequest);

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
    }

    @Test
    void exportTemplatePdf_NullResumeData_ThrowsIllegalArgumentException() {
        templateExportRequest.setResumeData(null);

        assertThrows(IllegalArgumentException.class, () ->
                resumePdfExportService.exportTemplatePdf(templateExportRequest)
        );
    }

    @Test
    void exportResumePdf_ServiceUnavailable_ThrowsIllegalStateException() {
        // Targets the initial branch condition inside fetchResume cleanly
        when(discoveryClient.getInstances("RESUME-SERVICE")).thenReturn(Collections.emptyList());

        assertThrows(IllegalStateException.class, () ->
                resumePdfExportService.exportResumePdf(1L, 1L)
        );
    }

    @Test
    void exportTemplatePdf_AllLayoutVariants_ExecuteToInsureHTMLBranches() {
        String[] variants = {"template-modern", "template-creative", "template-executive", "template-minimalist", "template-professional"};

        for (String var : variants) {
            templateExportRequest.setTemplateId(var);
            templateExportRequest.getStyleConfig().setVariant(var);

            byte[] pdfBytes = resumePdfExportService.exportTemplatePdf(templateExportRequest);

            assertNotNull(pdfBytes);
            assertTrue(pdfBytes.length > 0);
        }
    }
}