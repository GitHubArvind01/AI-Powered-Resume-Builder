package com.resumeai.aiservice.service;

import org.springframework.web.multipart.MultipartFile;

public interface ResumeTextExtractionService {

	String extractText(MultipartFile file);
}
