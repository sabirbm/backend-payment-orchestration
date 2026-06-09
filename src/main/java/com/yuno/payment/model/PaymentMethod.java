package com.yuno.payment.model;

/**
 * Represents the payment method type.
 * CARD payments are routed to Provider A.
 * UPI payments are routed to Provider B.
 */
public enum PaymentMethod {
    CARD,
    UPI
}
