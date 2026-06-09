package com.yuno.payment.model;

/**
 * Represents all possible states of a payment through its lifecycle.
 *
 * State transitions:
 * PENDING → PROCESSING → SUCCESS
 *                      → FAILED
 *                      → RETRYING → SUCCESS
 *                                 → FAILED
 */
public enum PaymentStatus {
    PENDING,      // Initial state when payment is created
    PROCESSING,   // Payment is being sent to the provider
    RETRYING,     // Previous attempt failed; retry is in progress
    SUCCESS,      // Payment was accepted by the provider
    FAILED        // All retry attempts exhausted or non-retryable failure
}
