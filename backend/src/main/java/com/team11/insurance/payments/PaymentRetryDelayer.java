package com.team11.insurance.payments;

/**
 * Strategy for delaying between payment gateway retries (Work Item: P4-17 / ADR-007).
 * <p>
 * Decouples backoff delays from hardcoded {@link Thread#sleep} so unit and integration
 * tests can verify retry attempt counts and backoff intervals without blocking for 7 seconds.
 */
@FunctionalInterface
public interface PaymentRetryDelayer {

    /**
     * Delay execution for the specified duration in milliseconds.
     *
     * @param delayMillis delay in milliseconds
     */
    void delay(long delayMillis);

    /**
     * Production delayer performing actual {@link Thread#sleep}.
     */
    PaymentRetryDelayer REAL = delayMillis -> {
        if (delayMillis > 0) {
            try {
                Thread.sleep(delayMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Payment retry backoff interrupted", e);
            }
        }
    };

    /**
     * No-op delayer for testing to avoid sleeping.
     */
    PaymentRetryDelayer NO_OP = delayMillis -> {
        // Immediate return
    };
}
