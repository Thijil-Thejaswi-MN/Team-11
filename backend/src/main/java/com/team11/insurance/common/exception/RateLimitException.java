package com.team11.insurance.common.exception;

import com.team11.insurance.common.api.ErrorCode;

/**
 * Thrown when rate limit is exceeded on login or quote endpoints (SAD Section 4.4: RATE-429).
 */
public class RateLimitException extends AppException {

    public RateLimitException() {
        super(ErrorCode.RATE_429, ErrorCode.RATE_429.getDefaultMessage());
    }

    public RateLimitException(String message) {
        super(ErrorCode.RATE_429, message);
    }
}
