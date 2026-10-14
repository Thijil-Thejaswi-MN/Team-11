package com.team11.insurance.common.api;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

/**
 * Standard Error Response schema mandated by SAD Section 4.4:
 * { timestamp, status, errorCode, message, details[], correlationId }
 */
public class ErrorResponse {

    private final Instant timestamp;
    private final int status;
    private final String errorCode;
    private final String message;
    private final List<String> details;
    private final String correlationId;

    public ErrorResponse(int status, String errorCode, String message, List<String> details, String correlationId) {
        this.timestamp = Instant.now();
        this.status = status;
        this.errorCode = errorCode;
        this.message = message;
        this.details = details != null ? List.copyOf(details) : Collections.emptyList();
        this.correlationId = correlationId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public int getStatus() {
        return status;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getMessage() {
        return message;
    }

    public List<String> getDetails() {
        return details;
    }

    public String getCorrelationId() {
        return correlationId;
    }
}
