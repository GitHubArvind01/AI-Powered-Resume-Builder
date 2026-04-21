package com.resumeai.export_service.mapper;

import com.resumeai.export_service.dto.ExportRequestDTO;
import com.resumeai.export_service.dto.ExportResponseDTO;
import com.resumeai.export_service.entity.ExportJob;

public class ExportMapper {

    public static ExportJob toEntity(ExportRequestDTO dto, String jobId, Long userId) {
        return ExportJob.builder()
                .jobId(jobId)
                .resumeId(dto.getResumeId())
                .userId(userId)
                .format(dto.getFormat())
                .status("QUEUED")
                .templateId(dto.getTemplateId())
                .customizations(dto.getCustomizations())
                .requestedAt(java.time.LocalDateTime.now())
                .expiresAt(java.time.LocalDateTime.now().plusDays(7))
                .build();
    }

    public static ExportResponseDTO toResponseDTO(ExportJob entity) {
        return ExportResponseDTO.builder()
                .jobId(entity.getJobId())
                .resumeId(entity.getResumeId())
                .userId(entity.getUserId())
                .format(entity.getFormat())
                .status(entity.getStatus())
                .fileUrl(entity.getFileUrl())
                .fileSizeKb(entity.getFileSizeKb())
                .templateId(entity.getTemplateId())
                .customizations(entity.getCustomizations())
                .requestedAt(entity.getRequestedAt())
                .completedAt(entity.getCompletedAt())
                .expiresAt(entity.getExpiresAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
