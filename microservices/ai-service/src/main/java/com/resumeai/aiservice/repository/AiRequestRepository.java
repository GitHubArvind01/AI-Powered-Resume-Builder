package com.resumeai.aiservice.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.resumeai.aiservice.entity.AiRequest;
import com.resumeai.aiservice.entity.AiRequest.RequestType;

@Repository
public interface AiRequestRepository extends JpaRepository<AiRequest, UUID> {

	List<AiRequest> findByUserId(Long userId);

	List<AiRequest> findByUserIdOrderByCreatedAtDesc(Long userId);

	Optional<AiRequest> findByRequestId(UUID requestId);

	@Query("SELECT COUNT(a) FROM AiRequest a WHERE a.userId = :userId AND a.createdAt >= :startOfMonth")
	Integer countByUserIdAndCreatedAtAfter(@Param("userId") Long userId,
			@Param("startOfMonth") LocalDateTime startOfMonth);

	@Query("SELECT COALESCE(SUM(a.tokensUsed), 0) FROM AiRequest a WHERE a.userId = :userId AND a.createdAt >= :startOfMonth")
	Integer sumTokensByUserIdAndCreatedAtAfter(@Param("userId") Long userId,
			@Param("startOfMonth") LocalDateTime startOfMonth);

	@Query("SELECT COALESCE(SUM(a.tokensUsed), 0) FROM AiRequest a WHERE a.userId = :userId")
	Integer sumTokensByUserId(@Param("userId") Long userId);

	List<AiRequest> findByUserIdAndRequestType(Long userId, RequestType requestType);

	@Query("SELECT a FROM AiRequest a WHERE a.userId = :userId AND a.status = 'FAILED'")
	List<AiRequest> findFailedRequestsByUserId(@Param("userId") Long userId);
}

