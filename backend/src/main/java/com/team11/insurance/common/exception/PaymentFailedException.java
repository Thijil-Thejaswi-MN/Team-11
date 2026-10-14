package com.team11.insurance.common.exception;

import com.team11.insurance.common.api.ErrorCode;

/**
 * Thrown when payment or payout fails (SAD Section 4.4: PAY-402).
 * Message clarifies the operation can be retried without double charging.
 */
public class PaymentFailedException extends AppException {

    public PaymentFailedException(String message) {
        super(ErrorCode.PAY_402, message);
    }
}
