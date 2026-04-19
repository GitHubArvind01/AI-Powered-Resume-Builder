package com.resumeai.resume_section_service.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResumeSectionRequestDTO {

    private Long sectionId;  // Optional: for bulk updates to identify which section to update

    @NotNull(message = "Resume ID is required")
    private Long resumeId;

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotBlank(message = "Section type is required")
    @Pattern(regexp = "SUMMARY|EXPERIENCE|EDUCATION|SKILLS|CERTIFICATIONS|PROJECTS|LANGUAGES|VOLUNTEER|CUSTOM",
             message = "Invalid section type")
    private String sectionType;

    @NotBlank(message = "Title is required")
    @Size(min = 1, max = 255, message = "Title must be between 1 and 255 characters")
    private String title;

    @Size(max = 50000, message = "Content cannot exceed 50000 characters")
    private String content;

    @Min(value = 0, message = "Display order must be non-negative")
    private Integer displayOrder;

    private Boolean isVisible = true;

    private Boolean aiGenerated = false;
}

