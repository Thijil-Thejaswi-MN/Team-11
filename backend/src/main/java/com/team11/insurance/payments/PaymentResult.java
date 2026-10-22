package com.team11.insurance.payments;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;

/**
 * Standard Payment Result DTO (Component C8 - SAD Table 3.4b, ADR-007, P4-03 Contract Section 3.2).
 * <p>
 * Supports both Clock-injected and explicit Instant factories to uphold the zero-system-time invariant.
 */
public record PaymentResult(
        Long paymentId,
        PaymentStatus status,
        String idempotencyKey,
        String transactionReference,
        BigDecimal amount,
        PaymentType type,
        Instant processedAt,
        String failureReason,
        String errorCode
) {
    public static PaymentResult success(Long paymentId, String key, String ref, BigDecimal amount, PaymentType type, Clock clock) {
        return new PaymentResult(paymentId, PaymentStatus.SUCCESS, key, ref, amount, type, clock.instant(), null, null);
    }

    public static PaymentResult success(Long paymentId, String key, String ref, BigDecimal amount, PaymentType type, Instant processedAt) {
        return new PaymentResult(paymentId, PaymentStatus.SUCCESS, key, ref, amount, type, processedAt, null, null);
    }

    public static PaymentResult failed(Long paymentId, String key, BigDecimal amount, PaymentType type, String reason, String code, Clock clock) {
        return new PaymentResult(paymentId, PaymentStatus.FAILED, key, null, amount, type, clock.instant(), reason, code);
    }

    public static PaymentResult failed(Long paymentId, String key, BigDecimal amount, PaymentType type, String reason, String code, Instant processedAt) {
        return new PaymentResult(paymentId, PaymentStatus.FAILED, key, null, amount, type, processedAt, reason, code);
    }
}
