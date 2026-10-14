package com.team11.insurance.common.exception;

import com.team11.insurance.common.api.ErrorCode;

import java.util.Collections;
import java.util.List;

/**
 * Base Application Runtime Exception for all domain and infrastructure errors.
 */
public abstract class AppException extends RuntimeException {

    private final ErrorCode errorCode;
    private final List<String> details;

    protected AppException(ErrorCode errorCode, String message) {
        this(errorCode, message, Collections.emptyList());
    }

    protected AppException(ErrorCode errorCode, String message, List<String> details) {
        super(message != null ? message : errorCode.getDefaultMessage());
        this.errorCode = errorCode;
        this.details = details != null ? List.copyOf(details) : Collections.emptyList();
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public List<String> getDetails() {
        return details;
    }
}
