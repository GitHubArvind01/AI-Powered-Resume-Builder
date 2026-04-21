package com.resumeai.payment_service.repository;

import com.resumeai.payment_service.entity.PaymentRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * PaymentRepository - Data access layer for Payment records
 */
@Repository
public interface PaymentRepository extends JpaRepository<PaymentRecord, UUID> {

    /**
     * Find a payment by PayPal payment ID
     */
    Optional<PaymentRecord> findByPaymentId(String paymentId);

    /**
     * Find all payments by user ID
     */
    List<PaymentRecord> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Find all payments by user ID and status
     */
    List<PaymentRecord> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, PaymentRecord.PaymentStatus status);

    /**
     * Count completed payments for a user
     */
    @Query("SELECT COUNT(p) FROM PaymentRecord p WHERE p.userId = :userId AND p.status = 'COMPLETED'")
    long countCompletedPaymentsByUserId(@Param("userId") Long userId);
}

