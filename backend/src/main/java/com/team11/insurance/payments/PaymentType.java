package com.team11.insurance.payments;

/**
 * Types of payment operations handled by the payment adapter (Component C8 - SAD Table 3.4b, P4-03 Contract Section 3.2).
 */
public enum PaymentType {
    CHARGE,
    REFUND,
    PAYOUT
}
