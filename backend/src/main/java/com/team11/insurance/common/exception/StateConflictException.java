package com.team11.insurance.common.exception;

import com.team11.insurance.common.api.ErrorCode;

/**
 * Thrown when operation is not allowed in current state (SAD Section 4.4: STATE-409).
 * E.g., paying an application that is not in Approved state.
 */
public class StateConflictException extends AppException {

    public StateConflictException(String message) {
        super(ErrorCode.STATE_409, message);
    }
}
