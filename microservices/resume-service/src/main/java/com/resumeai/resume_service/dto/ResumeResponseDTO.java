package com.resumeai.resume_service.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResumeResponseDTO {

    private Long id;

    private Long userId;

    private String title;

    private String content;

    private Boolean isPublic;

    private Integer viewCount;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private String status;

    private String description;
}

