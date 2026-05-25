package com.resumeai.export_service.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TemplateExportRequest {
    @NotBlank
    private String templateId;

    @NotBlank
    private String templateName;

    private String editorMode;

    @Valid
    @NotNull
    private TemplateResumeData resumeData;

    @Valid
    private TemplateStyleConfig styleConfig;
}
