//package com.resumeai.export_service.service;
//
//import com.resumeai.export_service.dto.*;
//import com.resumeai.export_service.exception.ResourceNotFoundException;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.cloud.client.DiscoveryClient;
//import org.springframework.cloud.client.ServiceInstance;
//import java.net.URI;
//import java.util.List;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.Mockito.mock;
//import static org.mockito.Mockito.when;
//
//@ExtendWith(MockitoExtension.class)
//class ResumePdfExportServiceTest {
//
//    @Mock
//    private DiscoveryClient discoveryClient;
//
//    @InjectMocks
//    private ResumePdfExportService resumePdfExportService;
//
//    private TemplateExportRequest templateExportRequest;
//
//    @BeforeEach
//    void setUp() {
//        templateExportRequest = new TemplateExportRequest();
//        templateExportRequest.setTemplateId("template-modern");
//        templateExportRequest.setTemplateName("Modern Variant");
//
//        TemplateResumeData resumeData = new TemplateResumeData();
//        TemplatePersonalInfo personalInfo = new TemplatePersonalInfo();
//        personalInfo.setFullName("Jane Smith");
//        personalInfo.setHeadline("Full Stack Engineer");
//        resumeData.setPersonalInfo(personalInfo);
//        resumeData.setSummary("Summary Text here");
//        templateExportRequest.setResumeData(resumeData);
//
//        // Safe setup using standard builders/setters to completely bypass any missing constructor issues
//        TemplateStyleConfig styleConfig = new TemplateStyleConfig();
//        styleConfig.setAccentColor("#38bdf8");
//        styleConfig.setVariant("modern");
//        templateExportRequest.setStyleConfig(styleConfig);
//    }
//
//    @Test
//    void exportTemplatePdf_ValidPayload_GeneratesBytesSuccessfully() {
//        byte[] pdfBytes = resumePdfExportService.exportTemplatePdf(templateExportRequest);
//
//        assertNotNull(pdfBytes);
//        assertTrue(pdfBytes.length > 0);
//    }
//
//    @Test
//    void exportTemplatePdf_NullResumeData_ThrowsIllegalArgumentException() {
//        templateExportRequest.setResumeData(null);
//
//        assertThrows(IllegalArgumentException.class, () ->
//                resumePdfExportService.exportTemplatePdf(templateExportRequest)
//        );
//    }
//
//    @Test
//    void exportResumePdf_DiscoveryClientEmpty_ThrowsIllegalStateException() {
//        // Correct signature match for discoveryClient target collections
//        when(discoveryClient.getInstances("RESUME-SERVICE")).thenReturn(List.of());
//
//        assertThrows(IllegalStateException.class, () ->
//                resumePdfExportService.exportResumePdf(10L, 1L)
//        );
//    }
//
//    @Test
//    void exportTemplatePdf_AllLayoutVariants_ExecuteToInsureHTMLBranches() {
//        String[] variants = {"template-modern", "template-creative", "template-executive", "template-minimalist", "template-professional"};
//
//        for (String var : variants) {
//            templateExportRequest.setTemplateId(var);
//            templateExportRequest.getStyleConfig().setVariant(var);
//            byte[] pdfBytes = resumePdfExportService.exportTemplatePdf(templateExportRequest);
//            assertNotNull(pdfBytes);
//            assertTrue(pdfBytes.length > 0);
//        }
//    }
//}