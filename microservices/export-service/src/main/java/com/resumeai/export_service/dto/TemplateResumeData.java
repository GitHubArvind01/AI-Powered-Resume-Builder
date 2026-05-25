package com.resumeai.export_service.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class TemplateResumeData {
    private String templateId;
    private String templateName;
    private String templateType;
    private String source;
    private TemplatePersonalInfo personalInfo;
    private String summary;
    private List<TemplateSectionData> experience = new ArrayList<>();
    private List<TemplateSectionData> education = new ArrayList<>();
    private List<String> skills = new ArrayList<>();
    private List<TemplateSectionData> projects = new ArrayList<>();
    private List<String> certifications = new ArrayList<>();
    private List<String> languages = new ArrayList<>();
}
