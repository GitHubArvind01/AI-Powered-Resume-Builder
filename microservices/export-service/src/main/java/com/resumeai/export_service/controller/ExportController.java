package com.resumeai.export_service.controller;

import com.resumeai.export_service.dto.ExportRequestDTO;
import com.resumeai.export_service.dto.ExportResponseDTO;
import com.resumeai.export_service.service.ExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/exports")
@RequiredArgsConstructor
public class ExportController {

    private final ExportService exportService;

    @PostMapping("/exportPdf")
    @Operation(summary = "Initiate PDF export")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Export initiated"),
            @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public ResponseEntity<ExportResponseDTO> exportPdf(@Valid @RequestBody ExportRequestDTO request, @RequestHeader("X-User-Id") Long userId) {
        request.setFormat("PDF");
        ExportResponseDTO response = exportService.initiateExport(request, userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/exportDocx")
    @Operation(summary = "Initiate DOCX export")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Export initiated"),
            @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public ResponseEntity<ExportResponseDTO> exportDocx(@Valid @RequestBody ExportRequestDTO request, @RequestHeader("X-User-Id") Long userId) {
        request.setFormat("DOCX");
        ExportResponseDTO response = exportService.initiateExport(request, userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/exportJson")
    @Operation(summary = "Initiate JSON export")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Export initiated"),
            @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public ResponseEntity<ExportResponseDTO> exportJson(@Valid @RequestBody ExportRequestDTO request, @RequestHeader("X-User-Id") Long userId) {
        request.setFormat("JSON");
        ExportResponseDTO response = exportService.initiateExport(request, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/jobStatus/{jobId}")
    @Operation(summary = "Get job status")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Job status retrieved"),
            @ApiResponse(responseCode = "404", description = "Job not found")
    })
    public ResponseEntity<ExportResponseDTO> getJobStatus(@PathVariable String jobId) {
        ExportResponseDTO response = exportService.getJobStatus(jobId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/byUser")
    @Operation(summary = "Get exports by user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Exports retrieved")
    })
    public ResponseEntity<List<ExportResponseDTO>> getExportsByUser(@RequestHeader("X-User-Id") Long userId) {
        List<ExportResponseDTO> responses = exportService.getExportsByUser(userId);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/download/{jobId}")
    @Operation(summary = "Get download URL")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Download URL retrieved"),
            @ApiResponse(responseCode = "404", description = "Job not found")
    })
    public ResponseEntity<String> getDownloadUrl(@PathVariable String jobId) {
        String url = exportService.getDownloadUrl(jobId);
        return ResponseEntity.ok(url);
    }

    @DeleteMapping("/{jobId}")
    @Operation(summary = "Delete export job")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Job deleted"),
            @ApiResponse(responseCode = "404", description = "Job not found")
    })
    public ResponseEntity<Void> deleteExport(@PathVariable String jobId) {
        exportService.deleteExport(jobId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stats")
    @Operation(summary = "Get export statistics")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Stats retrieved")
    })
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> stats = exportService.getStats();
        return ResponseEntity.ok(stats);
    }
}
