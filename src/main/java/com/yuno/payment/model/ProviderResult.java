package com.yuno.payment.model;

import lombok.Builder;
import lombok.Data;

/**
 * Internal model representing the outcome of a single provider call.
 * Used by the orchestration engine to decide on retry or failover.
 */
@Data
@Builder
public class ProviderResult {

    /**
     * Whether the provider accepted the payment request.
     */
    private boolean success;

    /**
     * Provider's transaction reference on success (e.g. "TXN-PROV-A-12345").
     */
    private String providerReference;

    /**
     * Human-readable error message on failure.
     */
    private String errorMessage;

    /**
     * Indicates if the failure is retryable.
     * Non-retryable errors: insufficient funds, invalid card, fraud block.
     * Retryable errors: timeout, provider unavailable, network error.
     */
    private boolean retryable;

    public static ProviderResult success(String reference) {
        return ProviderResult.builder()
                .success(true)
                .providerReference(reference)
                .retryable(false)
                .build();
    }

    public static ProviderResult retryableFailure(String errorMessage) {
        return ProviderResult.builder()
                .success(false)
                .errorMessage(errorMessage)
                .retryable(true)
                .build();
    }

    public static ProviderResult terminalFailure(String errorMessage) {
        return ProviderResult.builder()
                .success(false)
                .errorMessage(errorMessage)
                .retryable(false)
                .build();
    }
}
