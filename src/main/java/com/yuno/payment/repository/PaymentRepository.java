package com.yuno.payment.repository;

import com.yuno.payment.model.Payment;
import com.yuno.payment.model.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * JPA repository for Payment persistence.
 * Also serves as the idempotency store via idempotencyKey lookups.
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, String> {

    /**
     * Idempotency check: find existing payment by client-provided key.
     */
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    /**
     * Find all payments in a given status (useful for reprocessing stuck payments).
     */
    List<Payment> findByStatus(PaymentStatus status);

    /**
     * Find payments by customer for history/audit.
     */
    List<Payment> findByCustomerIdOrderByCreatedAtDesc(String customerId);

    /**
     * Count payments by status for metrics dashboard.
     */
    long countByStatus(PaymentStatus status);

    /**
     * Find payments for a specific provider.
     */
    List<Payment> findByAssignedProvider(String providerName);

    /**
     * Metrics: count payments created after a certain time.
     */
    @Query("SELECT COUNT(p) FROM Payment p WHERE p.createdAt >= :since")
    long countCreatedSince(Instant since);
}
