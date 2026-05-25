package com.resumeai.resume_service.controller;

import java.util.List;

import com.resumeai.resume_service.repository.ResumeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.resumeai.resume_service.dto.ResumeRequestDTO;
import com.resumeai.resume_service.dto.ResumeResponseDTO;
import com.resumeai.resume_service.service.ResumeService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/resumes")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Resume Management", description = "APIs for managing resumes")
public class ResumeResource {

    private final ResumeService resumeService;
    private final ResumeRepository resumeRepository;

    private static final String CREATE_RESUME_EXAMPLE = """
            {
                "userId": 1,
                "title": "My Resume",
                "content": "Experienced Software Developer...",
                "isPublic": false,
                "status": "DRAFT",
                "description": "My professional resume"
            }
            """;

    private static final String UPDATE_RESUME_EXAMPLE = """
            {
                "title": "Updated Resume",
                "content": "Updated content...",
                "isPublic": false,
                "status": "FINAL"
            }
            """;

    /**
     * Welcome endpoint to check if service is running
     */
    @GetMapping("/welcome")
    @Operation(summary = "Health Check", description = "Check if Resume Service is running")
    public ResponseEntity<String> welcome() {
        return ResponseEntity.ok("Resume Service is running!");
    }

     /**
      * Create a new resume
      * SECURITY: userId is extracted from X-User-Id header set by API Gateway
      */
     @PostMapping
     @Operation(
             summary = "Create Resume",
             description = "Create a new resume for the authenticated user. User ID is extracted from X-User-Id header (set by API Gateway).",
             requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                     required = true,
                     content = @Content(examples = {
                             @ExampleObject(name = "Create Resume Example", value = CREATE_RESUME_EXAMPLE)
                     })
             )
     )
     @ApiResponses(value = {
             @ApiResponse(responseCode = "201", description = "Resume created successfully"),
             @ApiResponse(responseCode = "400", description = "Invalid input data or missing X-User-Id header"),
             @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token"),
             @ApiResponse(responseCode = "500", description = "Internal server error")
     })
     public ResponseEntity<ResumeResponseDTO> createResume(
             @Valid @RequestBody ResumeRequestDTO resumeRequestDTO,
             @Parameter(description = "User ID from X-User-Id header (set by API Gateway)", example = "1")
             @RequestHeader("X-User-Id") Long userId) {

         if (userId == null) {
             throw new IllegalArgumentException("X-User-Id header is required");
         }

         ResumeResponseDTO createdResume = resumeService.createResume(resumeRequestDTO, userId);
         return ResponseEntity.status(HttpStatus.CREATED).body(createdResume);
     }

    /**
     * Get resume by ID
     */
    @GetMapping("/{id}")
    @Operation(
            summary = "Get Resume by ID",
            description = "Retrieve a specific resume by its ID"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Resume retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Resume not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ResumeResponseDTO> getResumeById(
            @Parameter(description = "Resume ID", example = "1")
            @PathVariable Long id) {
        ResumeResponseDTO resume = resumeService.getResumeById(id);
        resumeService.incrementViewCount(id);
        return ResponseEntity.ok(resume);
    }

    /**
     * Get all resumes for a user
     */
    @GetMapping("/user/{userId}")
    @Operation(
            summary = "Get Resumes by User ID",
            description = "Retrieve all resumes for a specific user"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Resumes retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<ResumeResponseDTO>> getResumesByUserId(
            @Parameter(description = "User ID", example = "1")
            @PathVariable Long userId) {
        List<ResumeResponseDTO> resumes = resumeService.getResumesByUserId(userId);
        return ResponseEntity.ok(resumes);
    }

    /**
     * Get all public resumes
     */
    @GetMapping("/public/all")
    @Operation(
            summary = "Get All Public Resumes",
            description = "Retrieve all public resumes"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Public resumes retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<ResumeResponseDTO>> getPublicResumes() {
        List<ResumeResponseDTO> resumes = resumeService.getPublicResumes();
        return ResponseEntity.ok(resumes);
    }

     /**
      * Update a resume
      * SECURITY: Ownership verification ensures user can only update their own resume
      */
     @PutMapping("/{id}")
     @Operation(
             summary = "Update Resume",
             description = "Update an existing resume. Only the resume owner can update it. User ID is extracted from X-User-Id header.",
             requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                     required = true,
                     content = @Content(examples = {
                             @ExampleObject(name = "Update Resume Example", value = UPDATE_RESUME_EXAMPLE)
                     })
             )
     )
     @ApiResponses(value = {
             @ApiResponse(responseCode = "200", description = "Resume updated successfully"),
             @ApiResponse(responseCode = "400", description = "Invalid input data or missing X-User-Id header"),
             @ApiResponse(responseCode = "403", description = "Forbidden - Resume does not belong to authenticated user"),
             @ApiResponse(responseCode = "404", description = "Resume not found"),
             @ApiResponse(responseCode = "500", description = "Internal server error")
     })
     public ResponseEntity<ResumeResponseDTO> updateResume(
             @Parameter(description = "Resume ID", example = "1")
             @PathVariable Long id,
             @Valid @RequestBody ResumeRequestDTO resumeRequestDTO,
             @Parameter(description = "User ID from X-User-Id header (set by API Gateway)", example = "1")
             @RequestHeader("X-User-Id") Long userId) {

         if (userId == null) {
             throw new IllegalArgumentException("X-User-Id header is required");
         }

         ResumeResponseDTO updatedResume = resumeService.updateResume(id, resumeRequestDTO, userId);
         return ResponseEntity.ok(updatedResume);
     }

     /**
      * Delete a resume
      * SECURITY: Ownership verification ensures user can only delete their own resume
      */
     @DeleteMapping("/{id}")
     @Operation(
             summary = "Delete Resume",
             description = "Delete a specific resume by its ID. Only the resume owner can delete it. User ID is extracted from X-User-Id header."
     )
     @ApiResponses(value = {
             @ApiResponse(responseCode = "204", description = "Resume deleted successfully"),
             @ApiResponse(responseCode = "400", description = "Missing X-User-Id header"),
             @ApiResponse(responseCode = "403", description = "Forbidden - Resume does not belong to authenticated user"),
             @ApiResponse(responseCode = "404", description = "Resume not found"),
             @ApiResponse(responseCode = "500", description = "Internal server error")
     })
     public ResponseEntity<Void> deleteResume(
             @Parameter(description = "Resume ID", example = "1")
             @PathVariable Long id,
             @Parameter(description = "User ID from X-User-Id header (set by API Gateway)", example = "1")
             @RequestHeader("X-User-Id") Long userId) {

         if (userId == null) {
             throw new IllegalArgumentException("X-User-Id header is required");
         }

         resumeService.deleteResume(id, userId);
         return ResponseEntity.noContent().build();
     }

    /**
     * Duplicate a resume
     */
    @PostMapping("/{id}/duplicate")
    @Operation(
            summary = "Duplicate Resume",
            description = "Create a copy of an existing resume with 'Copy of' prefix in the title"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Resume duplicated successfully"),
            @ApiResponse(responseCode = "404", description = "Resume not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ResumeResponseDTO> duplicateResume(
            @Parameter(description = "Resume ID", example = "1")
            @PathVariable Long id) {
        ResumeResponseDTO duplicatedResume = resumeService.duplicateResume(id);
        return ResponseEntity.status(HttpStatus.CREATED).body(duplicatedResume);
    }

    /**
     * Publish or unpublish a resume
     */
    @PutMapping("/{id}/publish")
    @Operation(
            summary = "Publish/Unpublish Resume",
            description = "Make a resume public or private"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Resume publication status updated successfully"),
            @ApiResponse(responseCode = "404", description = "Resume not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ResumeResponseDTO> publishResume(
            @Parameter(description = "Resume ID", example = "1")
            @PathVariable Long id,
            @Parameter(description = "Publish status", example = "true")
            @RequestParam Boolean isPublic) {
        ResumeResponseDTO publishedResume = resumeService.publishResume(id, isPublic);
        return ResponseEntity.ok(publishedResume);
    }

    /**
     * Get resumes by user and public status
     */
    @GetMapping("/user/{userId}/filter")
    @Operation(
            summary = "Get Resumes by User and Public Status",
            description = "Retrieve resumes for a specific user filtered by public status"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Resumes retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<ResumeResponseDTO>> getResumesByUserIdAndPublic(
            @Parameter(description = "User ID", example = "1")
            @PathVariable Long userId,
            @Parameter(description = "Is Public", example = "true")
            @RequestParam Boolean isPublic) {
        List<ResumeResponseDTO> resumes = resumeService.getResumesByUserIdAndPublic(userId, isPublic);
        return ResponseEntity.ok(resumes);
    }

    /**
     * Count resumes by user
     */
    @GetMapping("/user/{userId}/count")
    @Operation(
            summary = "Count Resumes by User",
            description = "Get the total count of resumes for a specific user"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Resume count retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Integer> countResumesByUserId(
            @Parameter(description = "User ID", example = "1")
            @PathVariable Long userId) {
        Integer count = resumeService.countResumesByUserId(userId);
        return ResponseEntity.ok(count);
    }

    /**
     * NEW ENDPOINT: Check if resume exists for a user (for service-to-service validation)
     * Used by resume-section-service to verify ownership before creating sections
     */
    @GetMapping("/{id}/user/{userId}/exists")
    @Operation(
            summary = "Check Resume Exists for User",
            description = "Check if a resume exists and belongs to a specific user. Used for service-to-service validation."
    )
    @ApiResponse(responseCode = "200", description = "Returns true if resume exists and belongs to user")
    public ResponseEntity<Boolean> resumeExistsForUser(
            @Parameter(description = "Resume ID", example = "1")
            @PathVariable Long id,
            @Parameter(description = "User ID", example = "1")
            @PathVariable Long userId) {
        boolean exists = resumeRepository.existsByIdAndUserId(id, userId);
        return ResponseEntity.ok(exists);
    }
}
