package com.yuno.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Yuno Payment Orchestration System.
 *
 * This application implements a simplified payment orchestration engine that:
 * - Routes CARD payments to Provider A
 * - Routes UPI payments to Provider B
 * - Supports retry and failover logic
 * - Enforces idempotency via unique idempotency keys
 * - Tracks payment status through its lifecycle
 */
@SpringBootApplication
public class PaymentOrchestrationApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentOrchestrationApplication.class, args);
    }
}
