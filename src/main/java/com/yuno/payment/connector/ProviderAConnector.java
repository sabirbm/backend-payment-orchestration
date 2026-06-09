package com.yuno.payment.connector;

import com.yuno.payment.model.Payment;
import com.yuno.payment.model.PaymentMethod;
import com.yuno.payment.model.ProviderResult;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Connector for Provider A — handles CARD payment processing.
 *
 * In production, this would use an HTTP client (RestTemplate/WebClient) to call
 * the provider's REST or SOAP API, with OAuth2/API key authentication.
 *
 * This implementation uses a configurable simulated failure rate to enable
 * realistic testing of retry and failover scenarios.
 *
 * Integration points:
 * - Input: Payment entity (amount, currency, customerId, paymentMethod)
 * - Output: ProviderResult with providerReference on success, error details on failure
 * - Provider base URL: configured via payment.provider.a.base-url
 */
@Component
public class ProviderAConnector implements PaymentProviderConnector {

    private static final Logger log = LoggerFactory.getLogger(ProviderAConnector.class);
    private static final String PROVIDER_NAME = "ProviderA";

    @Value("${payment.provider.a.failure-rate:0.2}")
    private double failureRate;

    @Value("${payment.provider.a.timeout-ms:5000}")
    private int timeoutMs;

    private final MeterRegistry meterRegistry;
    private volatile boolean available = true;

    public ProviderAConnector(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public PaymentMethod getSupportedMethod() {
        return PaymentMethod.CARD;
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    /**
     * Simulates submitting a CARD payment to Provider A.
     *
     * Simulation behavior:
     * - ~70% (configurable) of calls succeed
     * - ~15% are retryable (timeout / provider busy)
     * - ~15% are terminal (invalid card / fraud block)
     */
    @Override
    public ProviderResult processPayment(Payment payment) {
        log.info("[{}] Processing CARD payment id={} amount={} {}",
                PROVIDER_NAME, payment.getId(), payment.getAmount(), payment.getCurrency());

        Timer.Sample timerSample = Timer.start(meterRegistry);

        try {
            // Simulate network latency (50–300ms)
            simulateLatency();

            double roll = ThreadLocalRandom.current().nextDouble();

            if (roll < (1.0 - failureRate)) {
                // SUCCESS path
                String ref = "TXN-A-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                log.info("[{}] Payment succeeded paymentId={} ref={}", PROVIDER_NAME, payment.getId(), ref);
                meterRegistry.counter("provider.a.success").increment();
                return ProviderResult.success(ref);
            } else if (roll < (1.0 - failureRate / 2)) {
                // RETRYABLE FAILURE — simulates provider timeout or transient error
                String msg = "Provider A timeout: upstream payment gateway did not respond within " + timeoutMs + "ms";
                log.warn("[{}] Retryable failure paymentId={}: {}", PROVIDER_NAME, payment.getId(), msg);
                meterRegistry.counter("provider.a.failure.retryable").increment();
                return ProviderResult.retryableFailure(msg);
            } else {
                // TERMINAL FAILURE — simulates card decline, fraud block, etc.
                String msg = "Provider A declined: card declined due to insufficient funds or fraud detection";
                log.warn("[{}] Terminal failure paymentId={}: {}", PROVIDER_NAME, payment.getId(), msg);
                meterRegistry.counter("provider.a.failure.terminal").increment();
                return ProviderResult.terminalFailure(msg);
            }
        } finally {
            timerSample.stop(meterRegistry.timer("provider.a.latency"));
        }
    }

    private void simulateLatency() {
        try {
            Thread.sleep(ThreadLocalRandom.current().nextLong(50, 300));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // Package-private setter for testing
    void setAvailable(boolean available) {
        this.available = available;
    }

    void setFailureRate(double failureRate) {
        this.failureRate = failureRate;
    }
}
