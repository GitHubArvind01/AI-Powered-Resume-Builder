package com.resumeai.template_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "resume_templates")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResumeTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "template_id")
    private Integer templateId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Column(name = "html_layout", columnDefinition = "LONGTEXT")
    private String htmlLayout;

    @Column(name = "css_styles", columnDefinition = "LONGTEXT")
    private String cssStyles;

    @Column(name = "category", nullable = false)
    @Enumerated(EnumType.STRING)
    private TemplateCategory category;

    @Column(name = "is_premium", nullable = false)
    private boolean isPremium;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(name = "usage_count", nullable = false)
    private int usageCount;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

