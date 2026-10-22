package com.team11.insurance.common.exception;

import com.team11.insurance.common.api.ErrorCode;

/**
 * Thrown when physical storage operations encounter unexpected I/O failures (SAD Section 4.4: SYS-500).
 */
public class FileStorageException extends AppException {

    public FileStorageException(String message) {
        super(ErrorCode.SYS_500, message);
    }

    public FileStorageException(String message, Throwable cause) {
        super(ErrorCode.SYS_500, message);
        initCause(cause);
    }
}
