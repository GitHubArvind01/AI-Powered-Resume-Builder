package com.resumeai.export_service.service;

import com.resumeai.export_service.dto.ExportRequestDTO;
import com.resumeai.export_service.dto.ExportResponseDTO;
import com.resumeai.export_service.entity.ExportJob;
import com.resumeai.export_service.exception.ResourceNotFoundException;
import com.resumeai.export_service.mapper.ExportMapper;
import com.resumeai.export_service.repository.ExportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExportServiceImpl implements ExportService {

    private final ExportRepository exportRepository;
    private final RabbitTemplate rabbitTemplate;

    private static final String EXPORT_QUEUE = "export.queue";

    @Override
    public ExportResponseDTO initiateExport(ExportRequestDTO request, Long userId) {
        String jobId = UUID.randomUUID().toString();
        ExportJob job = ExportMapper.toEntity(request, jobId, userId);
        ExportJob savedJob = exportRepository.save(job);
        // Send to queue for async processing
        rabbitTemplate.convertAndSend(EXPORT_QUEUE, savedJob.getJobId());
        return ExportMapper.toResponseDTO(savedJob);
    }

    @Override
    public ExportResponseDTO getJobStatus(String jobId) {
        ExportJob job = exportRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Export job not found: " + jobId));
        return ExportMapper.toResponseDTO(job);
    }

    @Override
    public List<ExportResponseDTO> getExportsByUser(Long userId) {
        List<ExportJob> jobs = exportRepository.findByUserId(userId);
        return jobs.stream()
                .map(ExportMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public String getDownloadUrl(String jobId) {
        ExportJob job = exportRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Export job not found: " + jobId));
        return job.getFileUrl();
    }

    @Override
    public void deleteExport(String jobId) {
        if (!exportRepository.existsById(jobId)) {
            throw new ResourceNotFoundException("Export job not found: " + jobId);
        }
        exportRepository.deleteById(jobId);
    }

    @Override
    public Map<String, Object> getStats() {
        long totalExports = exportRepository.count();
        // Add more stats as needed
        return Map.of("totalExports", totalExports);
    }
}
