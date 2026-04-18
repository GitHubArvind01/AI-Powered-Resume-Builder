package com.resumeai.resume_section_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "resume_sections")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResumeSection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "section_id")
    private Long sectionId;

    @Column(name = "resume_id", nullable = false)
    private Long resumeId;

    @Column(name = "section_type", nullable = false, length = 50)
    private String sectionType;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "content", columnDefinition = "LONGTEXT")
    private String content;

    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(name = "is_visible")
    private Boolean isVisible = true;

    @Column(name = "ai_generated")
    private Boolean aiGenerated = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

