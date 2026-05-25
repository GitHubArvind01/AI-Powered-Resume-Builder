package com.resumeai.export_service.dto;

import java.util.List;
import java.util.Map;

import lombok.Data;

@Data
public class TemplateSectionData {
    private String title;
    private String subtitle;
    private String dateRange;
    private String description;
    private String location;
    private String link;
    private List<String> bullets;
    private Map<String, String> metadata;
}
