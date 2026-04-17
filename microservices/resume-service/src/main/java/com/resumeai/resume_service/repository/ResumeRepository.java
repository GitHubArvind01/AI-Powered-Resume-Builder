package com.resumeai.resume_service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.resumeai.resume_service.entity.Resume;

@Repository
public interface ResumeRepository extends JpaRepository<Resume, Long> {

    /**
     * Find all resumes by userId
     */
    List<Resume> findByUserId(Long userId);

    /**
     * Find all public resumes
     */
    List<Resume> findByIsPublicTrue();

    /**
     * Find all resumes by userId and isPublic status
     */
    List<Resume> findByUserIdAndIsPublic(Long userId, Boolean isPublic);

    /**
     * Check if resume exists for a user
     */
    Boolean existsByIdAndUserId(Long id, Long userId);

    /**
     * Find resume by id and userId (to ensure user ownership)
     */
    Optional<Resume> findByIdAndUserId(Long id, Long userId);

    /**
     * Count resumes by userId
     */
    Integer countByUserId(Long userId);

    /**
     * Find resumes by status
     */
    List<Resume> findByStatus(String status);
}

