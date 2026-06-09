package com.yuno.payment.controller;

import com.yuno.payment.model.CreatePaymentRequest;
import com.yuno.payment.model.PaymentResponse;
import com.yuno.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Payment Operations.
 *
 * API Endpoints:
 * - POST /api/v1/payments: Create and process a payment
 * - GET /api/v1/payments/{paymentId}: Fetch payment by ID
 *
 * Error Handling:
 * - Global exception handler in GlobalExceptionHandler
 * - Validation errors: 400 Bad Request
 * - Not found: 404 Not Found
 * - Idempotency conflict: 409 Conflict
 * - Server errors: 500 Internal Server Error
 */
@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * Create and process a payment.
     *
     * Request body example:
     * {
     *   "idempotencyKey": "550e8400-e29b-41d4-a716-446655440000",
     *   "amount": 100.00,
     *   "currency": "INR",
     *   "paymentMethod": "CARD",
     *   "customerId": "cust_12345",
     *   "customerName": "John Doe"
     * }
     *
     * Response on success: 201 CREATED
     * Response on duplicate idempotencyKey: 201 CREATED (cached response)
     *
     * @param request Payment creation request
     * @return PaymentResponse with ID, status, and provider reference
     */
    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(@Valid @RequestBody CreatePaymentRequest request) {
        log.info("Received payment creation request with idempotencyKey={}", request.getIdempotencyKey());

        PaymentResponse response = paymentService.createPayment(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /**
     * Fetch a payment by its ID.
     *
     * @param paymentId The payment ID returned from create
     * @return PaymentResponse with current status and details
     */
    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable String paymentId) {
        log.info("Received get payment request for paymentId={}", paymentId);

        PaymentResponse response = paymentService.getPayment(paymentId);

        return ResponseEntity.ok(response);
    }
}
