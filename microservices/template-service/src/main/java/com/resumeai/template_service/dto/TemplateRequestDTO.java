package com.resumeai.template_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TemplateRequestDTO {

    @NotBlank(message = "Template name is required")
    private String name;

    private String description;

    private String thumbnailUrl;

    @NotBlank(message = "HTML layout is required")
    private String htmlLayout;

    @NotBlank(message = "CSS styles are required")
    private String cssStyles;

    @NotNull(message = "Category is required")
    private String category;

    private boolean isPremium;

    private boolean isActive;
}

