package com.yuno.payment.model;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

/**
 * Standard error response envelope for all API errors.
 */
@Data
@Builder
public class ErrorResponse {

    private String errorCode;
    private String message;
    private List<String> details;
    private Instant timestamp;
    private String path;

    public static ErrorResponse of(String errorCode, String message, String path) {
        return ErrorResponse.builder()
                .errorCode(errorCode)
                .message(message)
                .timestamp(Instant.now())
                .path(path)
                .build();
    }

    public static ErrorResponse of(String errorCode, String message, List<String> details, String path) {
        return ErrorResponse.builder()
                .errorCode(errorCode)
                .message(message)
                .details(details)
                .timestamp(Instant.now())
                .path(path)
                .build();
    }
}
