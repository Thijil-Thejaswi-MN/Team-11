package com.team11.insurance.payments.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Request payload to initiate a payment (P4-17 / INS-F-009).
 * <p>
 * Security Invariant: The browser MUST send ONLY applicationId.
 * Never trust an amount or idempotency key supplied by the browser.
 */
public record PaymentInitiationRequest(
        @NotNull(message = "applicationId is required")
        @Positive(message = "applicationId must be positive")
        Long applicationId
) {}
