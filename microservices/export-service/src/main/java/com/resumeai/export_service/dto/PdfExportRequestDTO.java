package com.resumeai.export_service.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PdfExportRequestDTO {
    @NotNull(message = "Resume ID is required")
    private Long resumeId;
}
