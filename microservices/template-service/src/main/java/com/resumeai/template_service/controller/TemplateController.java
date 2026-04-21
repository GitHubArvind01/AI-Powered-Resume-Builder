package com.resumeai.template_service.controller;

import com.resumeai.template_service.dto.TemplateRequestDTO;
import com.resumeai.template_service.dto.TemplateResponseDTO;
import com.resumeai.template_service.service.TemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/templates")
@Tag(name = "Template Management", description = "APIs for managing resume templates")
public class TemplateController {

    private final TemplateService templateService;

    public TemplateController(TemplateService templateService) {
        this.templateService = templateService;
    }

    @PostMapping
    @Operation(summary = "Create a new template", description = "Creates a new resume template")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Template created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request body"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<TemplateResponseDTO> createTemplate(@Valid @RequestBody TemplateRequestDTO requestDTO) {
        TemplateResponseDTO response = templateService.createTemplate(requestDTO);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get template by ID", description = "Retrieves a template by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Template retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Template not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<TemplateResponseDTO> getTemplateById(@PathVariable Integer id) {
        TemplateResponseDTO response = templateService.getTemplateById(id);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping
    @Operation(summary = "Get all templates", description = "Retrieves all available templates")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Templates retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<TemplateResponseDTO>> getAllTemplates() {
        List<TemplateResponseDTO> response = templateService.getAllTemplates();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/free")
    @Operation(summary = "Get free templates", description = "Retrieves all non-premium templates")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Free templates retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<TemplateResponseDTO>> getFreeTemplates() {
        List<TemplateResponseDTO> response = templateService.getFreeTemplates();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/premium")
    @Operation(summary = "Get premium templates", description = "Retrieves all premium templates")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Premium templates retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<TemplateResponseDTO>> getPremiumTemplates() {
        List<TemplateResponseDTO> response = templateService.getPremiumTemplates();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/category/{category}")
    @Operation(summary = "Get templates by category", description = "Retrieves templates filtered by category")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Templates retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<TemplateResponseDTO>> getTemplatesByCategory(
            @PathVariable String category) {
        List<TemplateResponseDTO> response = templateService.getTemplatesByCategory(category);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/popular")
    @Operation(summary = "Get popular templates", description = "Retrieves templates ordered by usage count in descending order")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Popular templates retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<TemplateResponseDTO>> getPopularTemplates() {
        List<TemplateResponseDTO> response = templateService.getPopularTemplates();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update template", description = "Updates an existing template")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Template updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request body"),
            @ApiResponse(responseCode = "404", description = "Template not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<TemplateResponseDTO> updateTemplate(
            @PathVariable Integer id,
            @Valid @RequestBody TemplateRequestDTO requestDTO) {
        TemplateResponseDTO response = templateService.updateTemplate(id, requestDTO);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PutMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate template", description = "Soft deletes/deactivates a template")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Template deactivated successfully"),
            @ApiResponse(responseCode = "404", description = "Template not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Void> deactivateTemplate(@PathVariable Integer id) {
        templateService.deactivateTemplate(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PatchMapping("/{id}/increment-usage")
    @Operation(summary = "Increment usage count", description = "Increments the usageCount of a template")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Usage count incremented successfully"),
            @ApiResponse(responseCode = "404", description = "Template not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Void> incrementUsageCount(@PathVariable Integer id) {
        templateService.incrementUsageCount(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}

