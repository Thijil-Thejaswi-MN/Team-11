package com.team11.insurance.payments;

/**
 * Payment transaction outcome states (Component C8 - SAD Table 3.4b, ADR-007, P4-03 Contract Section 3.2).
 */
public enum PaymentStatus {
    SUCCESS,
    FAILED,
    PENDING
}
