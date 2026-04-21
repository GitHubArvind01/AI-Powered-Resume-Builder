package com.resumeai.export_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExportResponseDTO {

    private String jobId;
    private Long resumeId;
    private Long userId;
    private String format;
    private String status;
    private String fileUrl;
    private Long fileSizeKb;
    private Integer templateId;
    private String customizations;
    private LocalDateTime requestedAt;
    private LocalDateTime completedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
