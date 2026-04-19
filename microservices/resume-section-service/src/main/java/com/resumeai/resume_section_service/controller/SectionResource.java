package com.resumeai.resume_section_service.controller;

import com.resumeai.resume_section_service.dto.ResumeSectionRequestDTO;
import com.resumeai.resume_section_service.dto.ResumeSectionResponseDTO;
import com.resumeai.resume_section_service.service.SectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/sections")
@Tag(name = "Resume Sections", description = "APIs for managing resume sections")
public class SectionResource {

    private final SectionService sectionService;

    public SectionResource(SectionService sectionService) {
        this.sectionService = sectionService;
    }

    /**
     * Add a new resume section
     *
     * ✅ SECURITY: userId is extracted from X-User-Id header (set by API Gateway)
     * The request body contains only section-specific data, not userId
     */
    @PostMapping
    @Operation(summary = "Add a new resume section", description = "Creates a new section for a resume. User ID is extracted from X-User-Id header set by API Gateway.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Section created successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResumeSectionResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Resume does not belong to authenticated user"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ResumeSectionResponseDTO> addSection(
            @Valid @RequestBody ResumeSectionRequestDTO requestDTO,
            @Parameter(description = "User ID from X-User-Id header (set by API Gateway)", example = "1")
            @RequestHeader("X-User-Id") Long userId) {

        if (userId == null) {
            throw new IllegalArgumentException("X-User-Id header is required");
        }

        log.info("POST /api/v1/sections - Adding new section for user: {}", userId);
        ResumeSectionResponseDTO response = sectionService.addSection(requestDTO, userId);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Get section by ID
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get section by ID", description = "Retrieves a specific section by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Section found",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResumeSectionResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Section not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ResumeSectionResponseDTO> getSectionById(
            @Parameter(description = "Section ID", required = true) @PathVariable Long id) {
        log.info("GET /api/v1/sections/{} - Getting section", id);
        ResumeSectionResponseDTO response = sectionService.getSectionById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all sections for a resume
     */
    @GetMapping("/resume/{resumeId}")
    @Operation(summary = "Get all sections for a resume", description = "Retrieves all sections associated with a resume")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sections retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResumeSectionResponseDTO.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<ResumeSectionResponseDTO>> getSectionsByResume(
            @Parameter(description = "Resume ID", required = true) @PathVariable Long resumeId) {
        log.info("GET /api/v1/sections/resume/{} - Getting sections by resume", resumeId);
        List<ResumeSectionResponseDTO> response = sectionService.getSectionsByResumeOrderByDisplayOrder(resumeId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get sections by type
     */
    @GetMapping("/type")
    @Operation(summary = "Get sections by type", description = "Retrieves sections filtered by type")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sections retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResumeSectionResponseDTO.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<ResumeSectionResponseDTO>> getSectionsByType(
            @Parameter(description = "Resume ID", required = true) @RequestParam Long resumeId,
            @Parameter(description = "Section Type", required = true) @RequestParam String type) {
        log.info("GET /api/v1/sections/type - Getting sections by type: {}", type);
        List<ResumeSectionResponseDTO> response = sectionService.getSectionsByType(resumeId, type);
        return ResponseEntity.ok(response);
    }

    /**
     * Update a section
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update a section", description = "Updates an existing resume section")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Section updated successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResumeSectionResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "404", description = "Section not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ResumeSectionResponseDTO> updateSection(
            @Parameter(description = "Section ID", required = true) @PathVariable Long id,
            @Valid @RequestBody ResumeSectionRequestDTO requestDTO) {
        log.info("PUT /api/v1/sections/{} - Updating section", id);
        ResumeSectionResponseDTO response = sectionService.updateSection(id, requestDTO);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete a section
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a section", description = "Deletes a resume section by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Section deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Section not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Void> deleteSection(
            @Parameter(description = "Section ID", required = true) @PathVariable Long id) {
        log.info("DELETE /api/v1/sections/{} - Deleting section", id);
        sectionService.deleteSection(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Delete all sections for a resume
     */
    @DeleteMapping("/resume/{resumeId}")
    @Operation(summary = "Delete all sections for a resume", description = "Deletes all sections associated with a resume")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "All sections deleted successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Void> deleteAllSectionsByResume(
            @Parameter(description = "Resume ID", required = true) @PathVariable Long resumeId) {
        log.info("DELETE /api/v1/sections/resume/{} - Deleting all sections for resume", resumeId);
        sectionService.deleteAllSectionsByResume(resumeId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Toggle section visibility
     */
    @PatchMapping("/{id}/visibility")
    @Operation(summary = "Toggle section visibility", description = "Toggles the visibility status of a section")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Visibility toggled successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResumeSectionResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Section not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ResumeSectionResponseDTO> toggleVisibility(
            @Parameter(description = "Section ID", required = true) @PathVariable Long id) {
        log.info("PATCH /api/v1/sections/{}/visibility - Toggling visibility", id);
        ResumeSectionResponseDTO response = sectionService.toggleVisibility(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Reorder sections
     */
    @PatchMapping("/reorder")
    @Operation(summary = "Reorder sections", description = "Reorders sections based on provided order")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Sections reordered successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "404", description = "Resume or sections not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Void> reorderSections(
            @Parameter(description = "Resume ID", required = true) @RequestParam Long resumeId,
            @Parameter(description = "List of section IDs in desired order", required = true) @RequestBody List<Long> sectionIds) {
        log.info("PATCH /api/v1/sections/reorder - Reordering sections for resume: {}", resumeId);
        sectionService.reorderSections(resumeId, sectionIds);
        return ResponseEntity.noContent().build();
    }

    /**
     * Bulk update sections
     */
    @PutMapping("/bulk")
    @Operation(summary = "Bulk update sections", description = "Updates multiple sections at once")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sections updated successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResumeSectionResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "404", description = "One or more sections not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<ResumeSectionResponseDTO>> bulkUpdateSections(
            @Valid @RequestBody List<ResumeSectionRequestDTO> requestDTOs) {
        log.info("PUT /api/v1/sections/bulk - Bulk updating sections");
        List<ResumeSectionResponseDTO> response = sectionService.bulkUpdateSections(requestDTOs);
        return ResponseEntity.ok(response);
    }
}