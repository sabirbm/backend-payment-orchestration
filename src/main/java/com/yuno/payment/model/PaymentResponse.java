package com.yuno.payment.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Outbound DTO returned for both create and fetch payment operations.
 * Designed to be a stable API contract; internal fields like attemptCount
 * are included for observability and debugging.
 */
@Data
@Builder
public class PaymentResponse {

    private String paymentId;
    private String idempotencyKey;
    private BigDecimal amount;
    private String currency;
    private PaymentMethod paymentMethod;
    private PaymentStatus status;
    private String assignedProvider;
    private String providerReference;
    private String failureReason;
    private int attemptCount;
    private String customerId;
    private String customerName;
    private Instant createdAt;
    private Instant updatedAt;

    /**
     * Factory method to map domain entity to response DTO.
     */
    public static PaymentResponse from(Payment payment) {
        return PaymentResponse.builder()
                .paymentId(payment.getId())
                .idempotencyKey(payment.getIdempotencyKey())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .paymentMethod(payment.getPaymentMethod())
                .status(payment.getStatus())
                .assignedProvider(payment.getAssignedProvider())
                .providerReference(payment.getProviderReference())
                .failureReason(payment.getFailureReason())
                .attemptCount(payment.getAttemptCount())
                .customerId(payment.getCustomerId())
                .customerName(payment.getCustomerName())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }
}
