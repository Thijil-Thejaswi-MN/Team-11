package com.team11.insurance.common.exception;

import com.team11.insurance.common.api.ErrorCode;

import java.util.List;

/**
 * Thrown when input fails validation or business rules (SAD Section 4.4: VALID-422).
 */
public class ValidationException extends AppException {

    public ValidationException(String message) {
        super(ErrorCode.VALID_422, message);
    }

    public ValidationException(String message, List<String> details) {
        super(ErrorCode.VALID_422, message, details);
    }
}
