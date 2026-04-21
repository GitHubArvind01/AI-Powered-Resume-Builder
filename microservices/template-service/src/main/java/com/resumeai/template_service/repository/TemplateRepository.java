package com.resumeai.template_service.repository;

import com.resumeai.template_service.entity.ResumeTemplate;
import com.resumeai.template_service.entity.TemplateCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TemplateRepository extends JpaRepository<ResumeTemplate, Integer> {

    List<ResumeTemplate> findByCategory(TemplateCategory category);

    List<ResumeTemplate> findByIsPremium(boolean isPremium);

    List<ResumeTemplate> findByIsActive(boolean isActive);

    List<ResumeTemplate> findAllByOrderByUsageCountDesc();
}

