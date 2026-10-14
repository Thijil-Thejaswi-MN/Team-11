package com.team11.insurance.common.exception;

import com.team11.insurance.common.api.ErrorCode;

/**
 * Thrown when record does not exist or is not visible to this user (SAD Section 4.4: NOTFOUND-404).
 */
public class ResourceNotFoundException extends AppException {

    public ResourceNotFoundException(String resourceName, Object identifier) {
        super(ErrorCode.NOTFOUND_404, String.format("%s with identifier '%s' was not found", resourceName, identifier));
    }

    public ResourceNotFoundException(String message) {
        super(ErrorCode.NOTFOUND_404, message);
    }
}
