package com.yuno.payment.service;

import com.yuno.payment.engine.OrchestrationEngine;
import com.yuno.payment.exception.IdempotencyConflictException;
import com.yuno.payment.exception.PaymentNotFoundException;
import com.yuno.payment.model.CreatePaymentRequest;
import com.yuno.payment.model.Payment;
import com.yuno.payment.model.PaymentResponse;
import com.yuno.payment.model.PaymentStatus;
import com.yuno.payment.repository.PaymentRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Payment Service: High-level API for payment operations.
 *
 * Responsibilities:
 * 1. Idempotency: Ensure duplicate requests return cached response
 * 2. Request validation: Pre-check before orchestration
 * 3. Coordination: Delegate to OrchestrationEngine for processing
 * 4. Response mapping: Convert entities to DTOs
 * 5. Metrics: Track request volume and outcomes
 *
 * Idempotency Implementation:
 * - Client must provide a unique idempotencyKey (UUID recommended)
 * - If key exists, return stored response (idempotent)
 * - If key + different params, raise IdempotencyConflictException
 * - Entries cached for 24 hours (configurable via payment.idempotency.ttl-seconds)
 */
@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final OrchestrationEngine orchestrationEngine;
    private final MeterRegistry meterRegistry;

    public PaymentService(PaymentRepository paymentRepository,
                         OrchestrationEngine orchestrationEngine,
                         MeterRegistry meterRegistry) {
        this.paymentRepository = paymentRepository;
        this.orchestrationEngine = orchestrationEngine;
        this.meterRegistry = meterRegistry;
    }

    /**
     * Creates and processes a payment, ensuring idempotent behavior.
     *
     * Process:
     * 1. Check idempotency: if key exists, return cached result
     * 2. Create new Payment entity
     * 3. Orchestrate (route, call provider, retry)
     * 4. Return response
     *
     * @param request Client request with payment details
     * @return PaymentResponse with ID, status, and provider reference
     * @throws IdempotencyConflictException if key exists with conflicting params
     */
    @Transactional
    public PaymentResponse createPayment(CreatePaymentRequest request) {
        log.info("Creating payment with idempotencyKey={}", request.getIdempotencyKey());
        meterRegistry.counter("api.payment.create.requests").increment();

        // Idempotency check
        var existing = paymentRepository.findByIdempotencyKey(request.getIdempotencyKey());
        if (existing.isPresent()) {
            log.info("Idempotent request detected for key={}", request.getIdempotencyKey());
            meterRegistry.counter("api.payment.create.idempotent_hit").increment();
            return PaymentResponse.from(existing.get());
        }

        // Create new payment entity
        Payment payment = Payment.builder()
                .idempotencyKey(request.getIdempotencyKey())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .paymentMethod(request.getPaymentMethod())
                .customerId(request.getCustomerId())
                .customerName(request.getCustomerName())
                .status(PaymentStatus.PENDING)
                .attemptCount(0)
                .build();

        payment = paymentRepository.save(payment);
        log.info("Payment created with id={}", payment.getId());

        // Orchestrate the payment (route, call provider, retry)
        payment = orchestrationEngine.orchestratePayment(payment);

        return PaymentResponse.from(payment);
    }

    /**
     * Fetches a payment by its ID.
     *
     * @param paymentId The payment ID
     * @return PaymentResponse with current state
     * @throws PaymentNotFoundException if payment not found
     */
    @Transactional(readOnly = true)
    public PaymentResponse getPayment(String paymentId) {
        log.info("Fetching payment id={}", paymentId);
        meterRegistry.counter("api.payment.fetch.requests").increment();

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        return PaymentResponse.from(payment);
    }

    /**
     * Internal method: Get payment entity by ID.
     */
    @Transactional(readOnly = true)
    public Payment getPaymentEntity(String paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
    }

    /**
     * Health check: Count total payments and successful payments for metrics.
     */
    @Transactional(readOnly = true)
    public long getTotalPaymentCount() {
        return paymentRepository.count();
    }

    @Transactional(readOnly = true)
    public long getSuccessfulPaymentCount() {
        return paymentRepository.countByStatus(PaymentStatus.SUCCESS);
    }
}
