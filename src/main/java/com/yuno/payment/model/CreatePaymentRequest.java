package com.yuno.payment.model;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Inbound DTO for creating a new payment.
 * The idempotency key must be provided by the client (UUID recommended).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePaymentRequest {

    /**
     * Client-generated unique key for idempotency.
     * Duplicate requests with the same key within 24h return the original response.
     */
    @NotBlank(message = "Idempotency key is required")
    @Size(min = 8, max = 128, message = "Idempotency key must be between 8 and 128 characters")
    private String idempotencyKey;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @DecimalMax(value = "999999.99", message = "Amount must not exceed 999,999.99")
    @Digits(integer = 6, fraction = 4, message = "Invalid amount format")
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO 4217 code")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be uppercase ISO 4217 code (e.g. USD, INR)")
    private String currency;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    @NotBlank(message = "Customer ID is required")
    @Size(max = 256, message = "Customer ID must not exceed 256 characters")
    private String customerId;

    @Size(max = 256, message = "Customer name must not exceed 256 characters")
    private String customerName;


}
