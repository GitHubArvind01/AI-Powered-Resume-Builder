package com.resumeai.resume_service.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.resumeai.resume_service.exception.AccessDeniedException;
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
    public ResumeResponseDTO createResume(ResumeRequestDTO resumeRequestDTO, Long userId) {
        try {
            log.info("Creating resume for user ID: {}", userId);

            // ✅ VALIDATION: Verify that userId is not null (passed from API Gateway header)
            if (userId == null) {
                throw new IllegalArgumentException("User ID cannot be null");
            }

            // Create resume with userId from header (Gateway has already validated the JWT)
            Resume resume = ResumeMapper.toEntity(resumeRequestDTO);
            resume.setUserId(userId);
            resume.setCreatedAt(LocalDateTime.now());
            resume.setUpdatedAt(LocalDateTime.now());

            Resume savedResume = resumeRepository.save(resume);

            log.info("Resume created successfully with ID: {}", savedResume.getId());
            return ResumeMapper.toResponseDTO(savedResume);

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error creating resume for user ID: {}", userId, e);
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
    public ResumeResponseDTO updateResume(Long id, ResumeRequestDTO resumeRequestDTO, Long userId) {
        try {
            log.info("Updating resume with ID: {} for user ID: {}", id, userId);

            Resume resume = resumeRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Resume not found with ID: " + id));

            // ✅ SECURITY: Verify ownership before allowing update
            verifyResumeOwnership(resume, userId);

            ResumeMapper.updateEntityFromDTO(resumeRequestDTO, resume);
            Resume updatedResume = resumeRepository.save(resume);

            log.info("Resume updated successfully with ID: {}", id);
            return ResumeMapper.toResponseDTO(updatedResume);

        } catch (ResourceNotFoundException | AccessDeniedException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error updating resume with ID: {}", id, e);
            throw new ResumeServiceException("Failed to update resume: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteResume(Long id, Long userId) {
        try {
            log.info("Deleting resume with ID: {} for user ID: {}", id, userId);

            Resume resume = resumeRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Resume not found with ID: " + id));

            // ✅ VALIDATION 3: Verify ownership before deletion
            verifyResumeOwnership(resume, userId);

            resumeRepository.deleteById(id);
            log.info("Resume deleted successfully with ID: {}", id);

        } catch (ResourceNotFoundException | AccessDeniedException e) {
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


    /**
     * Verifies that a resume belongs to the specified user (ownership check)
     *
     * @param resume Resume entity
     * @param userId User ID claiming ownership
     * @throws AccessDeniedException if resume doesn't belong to the user
     */
    private void verifyResumeOwnership(Resume resume, Long userId) {
        if (!resume.getUserId().equals(userId)) {
            log.warn("Access denied: User ID: {} attempted to access resume ID: {} owned by user ID: {}",
                    userId, resume.getId(), resume.getUserId());
            throw new AccessDeniedException(
                    "Access denied: Resume with ID " + resume.getId() + " does not belong to user ID " + userId);
        }
        log.debug("Ownership verified for resume ID: {} by user ID: {}", resume.getId(), userId);
    }
}

