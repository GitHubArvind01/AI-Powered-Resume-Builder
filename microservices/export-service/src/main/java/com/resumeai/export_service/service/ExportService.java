package com.resumeai.export_service.service;

import com.resumeai.export_service.dto.ExportRequestDTO;
import com.resumeai.export_service.dto.ExportResponseDTO;

import java.util.List;
import java.util.Map;

public interface ExportService {

    ExportResponseDTO initiateExport(ExportRequestDTO request, Long userId);

    ExportResponseDTO getJobStatus(String jobId);

    List<ExportResponseDTO> getExportsByUser(Long userId);

    String getDownloadUrl(String jobId);

    void deleteExport(String jobId);

    Map<String, Object> getStats();
}
