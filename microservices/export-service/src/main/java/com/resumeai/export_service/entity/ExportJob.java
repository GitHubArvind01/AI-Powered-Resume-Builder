package com.resumeai.export_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "export_jobs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExportJob {

    @Id
    @Column(name = "job_id")
    private String jobId;

    @Column(name = "resume_id")
    private Long resumeId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "format")
    private String format;

    @Column(name = "status")
    private String status;

    @Column(name = "file_url")
    private String fileUrl;

    @Column(name = "file_size_kb")
    private Long fileSizeKb;

    @Column(name = "template_id")
    private Integer templateId;

    @Column(name = "customizations")
    private String customizations;

    @Column(name = "requested_at")
    private LocalDateTime requestedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
