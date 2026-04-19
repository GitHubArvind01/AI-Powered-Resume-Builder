package com.resumeai.resume_section_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResumeSectionResponseDTO {

    private Long sectionId;
    private Long resumeId;
    private String sectionType;
    private String title;
    private String content;
    private Integer displayOrder;
    private Boolean isVisible;
    private Boolean aiGenerated;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

