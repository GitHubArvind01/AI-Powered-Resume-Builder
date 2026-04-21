package com.resumeai.template_service.entity;

public enum TemplateCategory {
    PROFESSIONAL("Professional"),
    CREATIVE("Creative"),
    MODERN("Modern"),
    MINIMALIST("Minimalist"),
    ATS_OPTIMISED("ATS-Optimised");

    private final String displayName;

    TemplateCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

