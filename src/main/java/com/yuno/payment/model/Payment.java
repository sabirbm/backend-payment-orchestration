package com.yuno.payment.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * JPA entity representing a payment record in the persistence layer.
 * Each payment has a unique idempotency key to prevent duplicate processing.
 */
@Entity
@Table(name = "payments", indexes = {
    @Index(name = "idx_idempotency_key", columnList = "idempotencyKey", unique = true),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_created_at", columnList = "createdAt")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    /**
     * Client-provided key ensuring exactly-once processing semantics.
     * If a request with the same key is received, the stored response is returned.
     */
    @Column(nullable = false, unique = true, length = 128)
    private String idempotencyKey;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    /**
     * The provider this payment was routed to (ProviderA or ProviderB).
     */
    @Column(length = 64)
    private String assignedProvider;

    /**
     * Provider-returned transaction reference (set on SUCCESS).
     */
    @Column(length = 128)
    private String providerReference;

    /**
     * Description of last failure or error, if applicable.
     */
    @Column(length = 512)
    private String failureReason;

    /**
     * Number of provider call attempts made (including retries).
     */
    @Column(nullable = false)
    private int attemptCount;

    /**
     * Name of the customer initiating payment.
     */
    @Column(length = 256)
    private String customerName;

    /**
     * Customer identifier (e.g. user ID or email).
     */
    @Column(length = 256)
    private String customerId;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
