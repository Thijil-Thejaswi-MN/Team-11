package com.team11.insurance.common.exception;

import com.team11.insurance.common.api.ErrorCode;

/**
 * Thrown when role or ownership check fails (SAD Section 4.4: ACCESS-403).
 */
public class AccessDeniedException extends AppException {

    public AccessDeniedException() {
        super(ErrorCode.ACCESS_403, ErrorCode.ACCESS_403.getDefaultMessage());
    }

    public AccessDeniedException(String message) {
        super(ErrorCode.ACCESS_403, message);
    }
}
