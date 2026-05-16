package com.resumeai.export_service.dto;

import lombok.Data;

@Data
public class TemplatePersonalInfo {
    private String fullName;
    private String email;
    private String phone;
    private String location;
    private String headline;
    private String linkedin;
    private String github;
    private String portfolio;
}
