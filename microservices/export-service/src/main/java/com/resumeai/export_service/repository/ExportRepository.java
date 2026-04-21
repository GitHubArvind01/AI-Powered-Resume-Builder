package com.resumeai.export_service.repository;

import com.resumeai.export_service.entity.ExportJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ExportRepository extends JpaRepository<ExportJob, String> {

    List<ExportJob> findByUserId(Long userId);

    List<ExportJob> findByStatus(String status);

    @Query("SELECT e FROM ExportJob e WHERE e.expiresAt < :now")
    List<ExportJob> findExpiredJobs(@Param("now") LocalDateTime now);

    @Query("SELECT COUNT(e) FROM ExportJob e WHERE e.userId = :userId AND CAST(e.createdAt AS DATE) = CURRENT_DATE")
    long countByUserIdToday(@Param("userId") Long userId);
}
