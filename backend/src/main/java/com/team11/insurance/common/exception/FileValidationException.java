package com.team11.insurance.common.exception;

import com.team11.insurance.common.api.ErrorCode;

/**
 * Thrown when an uploaded file violates validation rules:
 * <ul>
 *   <li>File size exceeds 5 MB limit (SAD Section 4.4, STP TC-Sec-07: FILE-413 / HTTP 413)</li>
 *   <li>Disallowed MIME type or disguised file format (SAD Section 4.4, STP TC-Sec-07: FILE-415 / HTTP 415)</li>
 * </ul>
 */
public class FileValidationException extends AppException {

    public FileValidationException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
