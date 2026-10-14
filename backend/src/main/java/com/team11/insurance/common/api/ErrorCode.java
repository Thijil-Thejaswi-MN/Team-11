package com.team11.insurance.common.api;

import org.springframework.http.HttpStatus;

/**
 * Standard Error Codes from SAD Section 4.4
 * Project: Insurance Workflow Automation System
 */
public enum ErrorCode {

    VALID_422("VALID-422", HttpStatus.UNPROCESSABLE_ENTITY, "Input failed validation or a business rule."),
    AUTH_401("AUTH-401", HttpStatus.UNAUTHORIZED, "Missing, expired or invalid token, idle session (over 15 minutes), or wrong credentials."),
    AUTH_423("AUTH-423", HttpStatus.LOCKED, "Account locked after 5 failed logins for 15 minutes."),
    ACCESS_403("ACCESS-403", HttpStatus.FORBIDDEN, "Role or ownership check failed."),
    NOTFOUND_404("NOTFOUND-404", HttpStatus.NOT_FOUND, "Record does not exist (or is not visible to this user)."),
    STATE_409("STATE-409", HttpStatus.CONFLICT, "Operation not allowed in the current state."),
    PAY_402("PAY-402", HttpStatus.PAYMENT_REQUIRED, "Payment or payout failed; the message says it can be retried; no policy is issued."),
    RATE_429("RATE-429", HttpStatus.TOO_MANY_REQUESTS, "Too many requests on login or quote endpoints."),
    SYS_500("SYS-500", HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error. Please contact support.");

    private final String code;
    private final HttpStatus httpStatus;
    private final String defaultMessage;

    ErrorCode(String code, HttpStatus httpStatus, String defaultMessage) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
