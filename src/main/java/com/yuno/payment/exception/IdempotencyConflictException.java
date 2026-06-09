package com.yuno.payment.exception;

/**
 * Thrown when a duplicate idempotency key is detected but cannot be resolved
 * (e.g. key exists with different request parameters — a conflict).
 */
public class IdempotencyConflictException extends RuntimeException {

    public IdempotencyConflictException(String idempotencyKey) {
        super("Idempotency key already used with different payment parameters: " + idempotencyKey);
    }
}
