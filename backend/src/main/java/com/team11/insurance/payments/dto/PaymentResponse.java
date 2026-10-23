package com.team11.insurance.payments.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Response payload returned after initiating payment (P3 contract §3.2 & P4-17).
 * <p>
 * Canonical response format: { "paymentStatus": "Success", "policyNo": "POL-2026-000123" }
 */
public record PaymentResponse(
        String paymentStatus,
        String policyNo,
        String policyStatus,
        String transactionReference,
        BigDecimal amount,
        Instant processedAt
) {
    public static PaymentResponse success(String policyNo, String transactionRef, BigDecimal amount, Instant processedAt) {
        return new PaymentResponse("Success", policyNo, "ACTIVE", transactionRef, amount, processedAt);
    }
}
