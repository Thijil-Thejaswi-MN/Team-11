package com.team11.insurance.common.exception;

import com.team11.insurance.common.api.ErrorCode;

/**
 * Thrown on missing, expired, or invalid token, or wrong credentials (SAD Section 4.4: AUTH-401).
 */
public class AuthenticationException extends AppException {

    public AuthenticationException() {
        super(ErrorCode.AUTH_401, ErrorCode.AUTH_401.getDefaultMessage());
    }

    public AuthenticationException(String message) {
        super(ErrorCode.AUTH_401, message);
    }
}
