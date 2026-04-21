package com.resumeai.template_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TemplateResponseDTO {

    @JsonProperty("templateId")
    private Integer templateId;

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("thumbnailUrl")
    private String thumbnailUrl;

    @JsonProperty("htmlLayout")
    private String htmlLayout;

    @JsonProperty("cssStyles")
    private String cssStyles;

    @JsonProperty("category")
    private String category;

    @JsonProperty("isPremium")
    private boolean isPremium;

    @JsonProperty("isActive")
    private boolean isActive;

    @JsonProperty("usageCount")
    private int usageCount;

    @JsonProperty("createdAt")
    private LocalDateTime createdAt;
}

