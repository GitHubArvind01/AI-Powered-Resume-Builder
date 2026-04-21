package com.resumeai.export_service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExportRequestDTO {

    @NotNull(message = "Resume ID is required")
    private Long resumeId;

    @NotNull(message = "Format is required")
    @Pattern(regexp = "PDF|DOCX|JSON", message = "Format must be PDF, DOCX, or JSON")
    private String format;

    private Integer templateId;

    private String customizations;
}
