package com.yuno.payment.engine;

import com.yuno.payment.connector.PaymentProviderConnector;
import com.yuno.payment.model.Payment;
import com.yuno.payment.model.PaymentStatus;
import com.yuno.payment.model.ProviderResult;
import com.yuno.payment.repository.PaymentRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Orchestration Engine: Implements the core payment processing workflow.
 *
 * Responsibilities:
 * 1. Route payment to appropriate provider
 * 2. Execute provider call with retry logic
 * 3. Handle retryable vs terminal failures
 * 4. Update payment status through lifecycle
 * 5. Track metrics (success rate, latency, attempt count)
 *
 * State Machine:
 *   PENDING → PROCESSING → {SUCCESS, FAILED, RETRYING}
 *   RETRYING → PROCESSING → {SUCCESS, FAILED, RETRYING}
 *   SUCCESS / FAILED → terminal states
 *
 * Retry Logic:
 * - Only retryable failures are retried
 * - Exponential backoff: delay_ms, delay_ms * 2, delay_ms * 4, ...
 * - Max attempts configurable via payment.retry.max-attempts
 */
@Component
public class OrchestrationEngine {

    private static final Logger log = LoggerFactory.getLogger(OrchestrationEngine.class);

    private final RoutingEngine routingEngine;
    private final PaymentRepository paymentRepository;
    private final MeterRegistry meterRegistry;

    @Value("${payment.retry.max-attempts:3}")
    private int maxRetryAttempts;

    @Value("${payment.retry.delay-ms:500}")
    private long retryDelayMs;

    public OrchestrationEngine(RoutingEngine routingEngine,
                               PaymentRepository paymentRepository,
                               MeterRegistry meterRegistry) {
        this.routingEngine = routingEngine;
        this.paymentRepository = paymentRepository;
        this.meterRegistry = meterRegistry;
    }

    /**
     * Orchestrates the full payment lifecycle: routing, provider call, retry logic.
     *
     * @param payment Payment to process
     * @return Processed payment with updated status and provider reference
     */
    public Payment orchestratePayment(Payment payment) {
        log.info("Starting orchestration for payment id={} method={}", payment.getId(), payment.getPaymentMethod());

        // Update to PROCESSING
        payment.setStatus(PaymentStatus.PROCESSING);
        paymentRepository.save(payment);

        // Attempt payment with retry logic
        int attempt = 0;
        while (attempt < maxRetryAttempts) {
            attempt++;
            payment.setAttemptCount(attempt);

            log.info("Attempt {}/{} for payment id={}", attempt, maxRetryAttempts, payment.getId());

            PaymentProviderConnector provider = routingEngine.route(payment.getPaymentMethod());

            if (provider == null) {
                log.error("No available provider for payment id={}", payment.getId());
                payment.setStatus(PaymentStatus.FAILED);
                payment.setFailureReason("No available payment provider for method: " + payment.getPaymentMethod());
                paymentRepository.save(payment);
                meterRegistry.counter("orchestration.failed.no_provider").increment();
                return payment;
            }

            payment.setAssignedProvider(provider.getProviderName());

            // Call provider
            ProviderResult result = provider.processPayment(payment);

            if (result.isSuccess()) {
                // SUCCESS: Update payment and return
                payment.setStatus(PaymentStatus.SUCCESS);
                payment.setProviderReference(result.getProviderReference());
                payment.setFailureReason(null);
                paymentRepository.save(payment);

                log.info("Payment succeeded id={} ref={} after {} attempt(s)",
                        payment.getId(), result.getProviderReference(), attempt);
                meterRegistry.counter("orchestration.success").increment();
                meterRegistry.summary("orchestration.attempts", "status", "success").record(attempt);
                return payment;
            }

            // FAILURE: Check if retryable
            if (!result.isRetryable()) {
                // Terminal failure: stop immediately
                payment.setStatus(PaymentStatus.FAILED);
                payment.setFailureReason(result.getErrorMessage());
                paymentRepository.save(payment);

                log.warn("Payment failed (non-retryable) id={} after {} attempt(s): {}",
                        payment.getId(), attempt, result.getErrorMessage());
                meterRegistry.counter("orchestration.failed.terminal").increment();
                return payment;
            }

            // Retryable failure: check if we have more attempts
            if (attempt < maxRetryAttempts) {
                payment.setStatus(PaymentStatus.RETRYING);
                payment.setFailureReason(result.getErrorMessage());
                paymentRepository.save(payment);

                // Exponential backoff: delay increases with each retry
                long delayMs = retryDelayMs * ((long) Math.pow(2, attempt - 1));
                log.info("Retryable failure for payment id={}. Waiting {} ms before retry...",
                        payment.getId(), delayMs);

                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("Retry sleep interrupted for payment id={}", payment.getId());
                }

                meterRegistry.counter("orchestration.retry").increment();
            }
        }

        // Exhausted all retries
        payment.setStatus(PaymentStatus.FAILED);
        payment.setFailureReason("All retry attempts exhausted");
        paymentRepository.save(payment);

        log.error("Payment exhausted all retries id={} after {} attempt(s)", payment.getId(), attempt);
        meterRegistry.counter("orchestration.failed.exhausted_retries").increment();
        return payment;
    }

    // Getters for testing
    public int getMaxRetryAttempts() {
        return maxRetryAttempts;
    }

    public long getRetryDelayMs() {
        return retryDelayMs;
    }
}
