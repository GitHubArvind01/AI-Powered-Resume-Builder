package com.resumeai.export_service.dto;

import lombok.Data;

@Data
public class TemplateStyleConfig {
    private String variant;
    private String theme;
    private String accentColor;
    private String fontFamily;
    private Boolean compactSpacing;
    private Boolean singlePage;
}
