package com.resumeai.export_service.controller;

import com.resumeai.export_service.dto.PdfExportRequestDTO;
import com.resumeai.export_service.service.ResumePdfExportService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/export")
@RequiredArgsConstructor
public class PdfExportController {

    private final ResumePdfExportService resumePdfExportService;

    @PostMapping("/pdf")
    @Operation(summary = "Export resume as PDF", description = "Generate a PDF byte stream for the authenticated user's resume.")
    public ResponseEntity<byte[]> exportResumePdf(
            @Valid @RequestBody PdfExportRequestDTO request,
            @RequestHeader("X-User-Id") Long userId) {
        byte[] pdfBytes = resumePdfExportService.exportResumePdf(request.getResumeId(), userId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=resume-" + request.getResumeId() + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}
