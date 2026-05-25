package com.resumeai.export_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TemplateStyleConfig {
    private String variant;
    private String theme;
    private String accentColor;
    private String fontFamily;
    private Boolean compactSpacing;
    private Boolean singlePage;
}
