package com.team11.insurance.common.exception;

import com.team11.insurance.common.api.ErrorCode;

/**
 * Thrown when an account is locked after 5 failed login attempts for 15 minutes (SAD Section 4.4: AUTH-423).
 */
public class AccountLockedException extends AppException {

    public AccountLockedException() {
        super(ErrorCode.AUTH_423, ErrorCode.AUTH_423.getDefaultMessage());
    }

    public AccountLockedException(String message) {
        super(ErrorCode.AUTH_423, message);
    }
}
