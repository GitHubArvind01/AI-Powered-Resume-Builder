package com.resumeai.resume_service.mapper;

import java.time.LocalDateTime;

import com.resumeai.resume_service.dto.ResumeRequestDTO;
import com.resumeai.resume_service.dto.ResumeResponseDTO;
import com.resumeai.resume_service.entity.Resume;

public class ResumeMapper {

    private ResumeMapper() {
        // Private constructor to prevent instantiation
    }

    /**
     * Convert ResumeRequestDTO to Resume Entity
     */
    public static Resume toEntity(ResumeRequestDTO dto) {
        if (dto == null) {
            return null;
        }

        return Resume.builder()
                .userId(dto.getUserId())
                .title(dto.getTitle())
                .content(dto.getContent())
                .isPublic(dto.getIsPublic() != null ? dto.getIsPublic() : false)
                .status(dto.getStatus())
                .description(dto.getDescription())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .viewCount(0)
                .build();
    }

    /**
     * Convert Resume Entity to ResumeResponseDTO
     */
    public static ResumeResponseDTO toResponseDTO(Resume entity) {
        if (entity == null) {
            return null;
        }

        return ResumeResponseDTO.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .title(entity.getTitle())
                .content(entity.getContent())
                .isPublic(entity.getIsPublic())
                .viewCount(entity.getViewCount())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .status(entity.getStatus())
                .description(entity.getDescription())
                .build();
    }

    /**
     * Partially update entity from request DTO
     */
    public static void updateEntityFromDTO(ResumeRequestDTO dto, Resume entity) {
        if (dto == null || entity == null) {
            return;
        }

        if (dto.getTitle() != null) {
            entity.setTitle(dto.getTitle());
        }
        if (dto.getContent() != null) {
            entity.setContent(dto.getContent());
        }
        if (dto.getIsPublic() != null) {
            entity.setIsPublic(dto.getIsPublic());
        }
        if (dto.getStatus() != null) {
            entity.setStatus(dto.getStatus());
        }
        if (dto.getDescription() != null) {
            entity.setDescription(dto.getDescription());
        }
        entity.setUpdatedAt(LocalDateTime.now());
    }
}

