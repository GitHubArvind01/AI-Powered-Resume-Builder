package com.resumeai.resume_section_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Feign Client for Resume Service
 * Provides service-to-service communication to validate resume ownership
 */
@FeignClient(name = "resume-service", url = "${feign.resume-service.url:http://localhost:8081}")
public interface ResumeServiceClient {

    /**
     * Check if a resume exists and belongs to a specific user
     *
     * @param resumeId the ID of the resume to check
     * @param userId the ID of the user who should own the resume
     * @return true if the resume exists and belongs to the user, false otherwise
     */
    @GetMapping("/api/v1/resumes/{id}/user/{userId}/exists")
    Boolean resumeExistsForUser(
            @PathVariable("id") Long resumeId,
            @PathVariable("userId") Long userId
    );
}

