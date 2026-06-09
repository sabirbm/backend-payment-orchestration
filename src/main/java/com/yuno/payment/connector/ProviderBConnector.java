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

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Connector for Provider B — handles UPI payment processing.
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
 * - Provider base URL: configured via payment.provider.b.base-url
 */
@Component
public class ProviderBConnector implements PaymentProviderConnector {

    private static final Logger log = LoggerFactory.getLogger(ProviderBConnector.class);
    private static final String PROVIDER_NAME = "ProviderB";

    @Value("${payment.provider.b.failure-rate:0.1}")
    private double failureRate;

    @Value("${payment.provider.b.timeout-ms:5000}")
    private int timeoutMs;

    private final MeterRegistry meterRegistry;
    private volatile boolean available = true;

    public ProviderBConnector(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public PaymentMethod getSupportedMethod() {
        return PaymentMethod.UPI;
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    /**
     * Simulates submitting a UPI payment to Provider B.
     *
     * Simulation behavior:
     * - ~90% (configurable) of calls succeed
     * - ~5% are retryable (timeout / provider busy)
     * - ~5% are terminal (user declined / invalid UPI)
     */
    @Override
    public ProviderResult processPayment(Payment payment) {
        log.info("[{}] Processing UPI payment id={} amount={} {}",
                PROVIDER_NAME, payment.getId(), payment.getAmount(), payment.getCurrency());

        Timer.Sample timerSample = Timer.start(meterRegistry);

        try {
            // Simulate network latency (30–200ms, faster than Provider A)
            simulateLatency();

            double roll = ThreadLocalRandom.current().nextDouble();

            if (roll < (1.0 - failureRate)) {
                // SUCCESS path
                String ref = "UPI-B-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                log.info("[{}] Payment succeeded paymentId={} ref={}", PROVIDER_NAME, payment.getId(), ref);
                meterRegistry.counter("provider.b.success").increment();
                return ProviderResult.success(ref);
            } else if (roll < (1.0 - failureRate / 2)) {
                // RETRYABLE FAILURE — simulates provider timeout or user action pending
                String msg = "Provider B timeout: UPI verification took too long, please retry";
                log.warn("[{}] Retryable failure paymentId={}: {}", PROVIDER_NAME, payment.getId(), msg);
                meterRegistry.counter("provider.b.failure.retryable").increment();
                return ProviderResult.retryableFailure(msg);
            } else {
                // TERMINAL FAILURE — simulates user decline or invalid UPI
                String msg = "Provider B declined: user declined payment or invalid UPI handle";
                log.warn("[{}] Terminal failure paymentId={}: {}", PROVIDER_NAME, payment.getId(), msg);
                meterRegistry.counter("provider.b.failure.terminal").increment();
                return ProviderResult.terminalFailure(msg);
            }
        } finally {
            timerSample.stop(meterRegistry.timer("provider.b.latency"));
        }
    }

    private void simulateLatency() {
        try {
            Thread.sleep(ThreadLocalRandom.current().nextLong(30, 200));
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
