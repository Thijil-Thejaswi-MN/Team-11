package com.team11.insurance.payments.simulator;

/**
 * Exception representing transient network/gateway read timeout for retry verification.
 */
public class PaymentTimeoutException extends Exception {
    public PaymentTimeoutException(String message) {
        super(message);
    }
}
