package com.yuno.payment.exception;

/**
 * Thrown when all providers for a payment method are unavailable
 * and failover has been exhausted.
 */
public class ProviderUnavailableException extends RuntimeException {

    public ProviderUnavailableException(String message) {
        super(message);
    }
}
