package com.yuno.payment.config;

import com.yuno.payment.exception.IdempotencyConflictException;
import com.yuno.payment.exception.PaymentNotFoundException;
import com.yuno.payment.exception.ProviderUnavailableException;
import com.yuno.payment.model.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.ArrayList;
import java.util.List;

/**
 * Global exception handler for all REST endpoints.
 *
 * Maps domain exceptions to HTTP responses:
 * - PaymentNotFoundException → 404 Not Found
 * - IdempotencyConflictException → 409 Conflict
 * - ProviderUnavailableException → 503 Service Unavailable
 * - Validation errors → 400 Bad Request
 * - Generic exceptions → 500 Internal Server Error
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(PaymentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePaymentNotFound(
            PaymentNotFoundException ex,
            WebRequest request) {

        ErrorResponse error = ErrorResponse.of(
                "PAYMENT_NOT_FOUND",
                ex.getMessage(),
                request.getDescription(false).replace("uri=", "")
        );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(error);
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    public ResponseEntity<ErrorResponse> handleIdempotencyConflict(
            IdempotencyConflictException ex,
            WebRequest request) {

        ErrorResponse error = ErrorResponse.of(
                "IDEMPOTENCY_CONFLICT",
                ex.getMessage(),
                request.getDescription(false).replace("uri=", "")
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(error);
    }

    @ExceptionHandler(ProviderUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleProviderUnavailable(
            ProviderUnavailableException ex,
            WebRequest request) {

        ErrorResponse error = ErrorResponse.of(
                "PROVIDER_UNAVAILABLE",
                ex.getMessage(),
                request.getDescription(false).replace("uri=", "")
        );

        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex,
            WebRequest request) {

        List<String> details = new ArrayList<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String message = error.getDefaultMessage();
            details.add(fieldName + ": " + message);
        });

        ErrorResponse error = ErrorResponse.of(
                "VALIDATION_ERROR",
                "Request validation failed",
                details,
                request.getDescription(false).replace("uri=", "")
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(
            Exception ex,
            WebRequest request) {

        log.error("Unhandled exception occurred", ex);

        ErrorResponse error = ErrorResponse.of(
                "INTERNAL_SERVER_ERROR",
                "An unexpected error occurred. Please contact support.",
                request.getDescription(false).replace("uri=", "")
        );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error);
    }
}
