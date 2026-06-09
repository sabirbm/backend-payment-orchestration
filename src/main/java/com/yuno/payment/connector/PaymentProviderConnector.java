package com.yuno.payment.connector;

import com.yuno.payment.model.Payment;
import com.yuno.payment.model.PaymentMethod;
import com.yuno.payment.model.ProviderResult;

/**
 * Contract for all payment provider connectors.
 *
 * Each connector encapsulates the integration logic for one payment provider,
 * including authentication, request/response mapping, and error classification.
 *
 * Integration Points:
 * - ProviderAConnector: handles CARD payments
 * - ProviderBConnector: handles UPI payments
 *
 * In production, each connector would maintain an HTTP client, circuit breaker,
 * and provider-specific auth token management.
 */
public interface PaymentProviderConnector {

    /**
     * Returns the unique name of this provider (used in routing and logging).
     */
    String getProviderName();

    /**
     * Returns the payment method this provider primarily handles.
     * Used by the routing engine for initial assignment.
     */
    PaymentMethod getSupportedMethod();

    /**
     * Submits a payment to the provider.
     *
     * @param payment The payment entity to process
     * @return ProviderResult indicating success or failure details
     */
    ProviderResult processPayment(Payment payment);

    /**
     * Checks if this provider is currently healthy/available.
     * Used by the failover mechanism before routing.
     */
    boolean isAvailable();
}
