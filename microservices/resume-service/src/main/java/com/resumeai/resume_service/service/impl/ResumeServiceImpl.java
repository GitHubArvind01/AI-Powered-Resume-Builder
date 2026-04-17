package com.resumeai.resume_service.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.resumeai.resume_service.dto.ResumeRequestDTO;
import com.resumeai.resume_service.dto.ResumeResponseDTO;
import com.resumeai.resume_service.entity.Resume;
import com.resumeai.resume_service.exception.ResourceNotFoundException;
import com.resumeai.resume_service.exception.ResumeServiceException;
import com.resumeai.resume_service.mapper.ResumeMapper;
import com.resumeai.resume_service.repository.ResumeRepository;
import com.resumeai.resume_service.service.ResumeService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ResumeServiceImpl implements ResumeService {

    private final ResumeRepository resumeRepository;

    @Override
    public ResumeResponseDTO createResume(ResumeRequestDTO resumeRequestDTO) {
        try {
            log.info("Creating resume for user ID: {}", resumeRequestDTO.getUserId());

            Resume resume = ResumeMapper.toEntity(resumeRequestDTO);
            Resume savedResume = resumeRepository.save(resume);

            log.info("Resume created successfully with ID: {}", savedResume.getId());
            return ResumeMapper.toResponseDTO(savedResume);

        } catch (Exception e) {
            log.error("Error creating resume for user ID: {}", resumeRequestDTO.getUserId(), e);
            throw new ResumeServiceException("Failed to create resume: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ResumeResponseDTO getResumeById(Long id) {
        log.info("Fetching resume with ID: {}", id);

        Resume resume = resumeRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Resume not found with ID: {}", id);
                    return new ResourceNotFoundException("Resume not found with ID: " + id);
                });

        return ResumeMapper.toResponseDTO(resume);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResumeResponseDTO> getResumesByUserId(Long userId) {
        log.info("Fetching all resumes for user ID: {}", userId);

        List<Resume> resumes = resumeRepository.findByUserId(userId);
        return resumes.stream()
                .map(ResumeMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResumeResponseDTO> getPublicResumes() {
        log.info("Fetching all public resumes");

        List<Resume> resumes = resumeRepository.findByIsPublicTrue();
        return resumes.stream()
                .map(ResumeMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public ResumeResponseDTO updateResume(Long id, ResumeRequestDTO resumeRequestDTO) {
        try {
            log.info("Updating resume with ID: {}", id);

            Resume resume = resumeRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Resume not found with ID: " + id));

            ResumeMapper.updateEntityFromDTO(resumeRequestDTO, resume);
            Resume updatedResume = resumeRepository.save(resume);

            log.info("Resume updated successfully with ID: {}", id);
            return ResumeMapper.toResponseDTO(updatedResume);

        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error updating resume with ID: {}", id, e);
            throw new ResumeServiceException("Failed to update resume: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteResume(Long id) {
        try {
            log.info("Deleting resume with ID: {}", id);

            if (!resumeRepository.existsById(id)) {
                throw new ResourceNotFoundException("Resume not found with ID: " + id);
            }

            resumeRepository.deleteById(id);
            log.info("Resume deleted successfully with ID: {}", id);

        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error deleting resume with ID: {}", id, e);
            throw new ResumeServiceException("Failed to delete resume: " + e.getMessage(), e);
        }
    }

    @Override
    public ResumeResponseDTO duplicateResume(Long id) {
        try {
            log.info("Duplicating resume with ID: {}", id);

            Resume originalResume = resumeRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Resume not found with ID: " + id));

            // Create a deep copy of the resume
            Resume duplicatedResume = Resume.builder()
                    .userId(originalResume.getUserId())
                    .title("Copy of " + originalResume.getTitle())
                    .content(originalResume.getContent())
                    .isPublic(false) // New copy is private by default
                    .status(originalResume.getStatus())
                    .description(originalResume.getDescription())
                    .viewCount(0)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            Resume savedResume = resumeRepository.save(duplicatedResume);

            log.info("Resume duplicated successfully. Original ID: {}, New ID: {}", id, savedResume.getId());
            return ResumeMapper.toResponseDTO(savedResume);

        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error duplicating resume with ID: {}", id, e);
            throw new ResumeServiceException("Failed to duplicate resume: " + e.getMessage(), e);
        }
    }

    @Override
    public ResumeResponseDTO publishResume(Long id, Boolean isPublic) {
        try {
            log.info("Publishing resume with ID: {} (isPublic: {})", id, isPublic);

            Resume resume = resumeRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Resume not found with ID: " + id));

            resume.setIsPublic(isPublic);
            resume.setUpdatedAt(LocalDateTime.now());
            Resume updatedResume = resumeRepository.save(resume);

            log.info("Resume publish status updated successfully with ID: {}", id);
            return ResumeMapper.toResponseDTO(updatedResume);

        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error publishing resume with ID: {}", id, e);
            throw new ResumeServiceException("Failed to publish resume: " + e.getMessage(), e);
        }
    }

    @Override
    public void incrementViewCount(Long id) {
        try {
            log.debug("Incrementing view count for resume ID: {}", id);

            Resume resume = resumeRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Resume not found with ID: " + id));

            resume.setViewCount(resume.getViewCount() + 1);
            resume.setUpdatedAt(LocalDateTime.now());
            resumeRepository.save(resume);

        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error incrementing view count for resume ID: {}", id, e);
            throw new ResumeServiceException("Failed to increment view count: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResumeResponseDTO> getResumesByUserIdAndPublic(Long userId, Boolean isPublic) {
        log.info("Fetching resumes for user ID: {} with public status: {}", userId, isPublic);

        List<Resume> resumes = resumeRepository.findByUserIdAndIsPublic(userId, isPublic);
        return resumes.stream()
                .map(ResumeMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Integer countResumesByUserId(Long userId) {
        log.info("Counting resumes for user ID: {}", userId);
        return resumeRepository.countByUserId(userId);
    }
}

