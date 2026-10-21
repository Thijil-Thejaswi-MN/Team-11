package com.team11.insurance.applications.exception;

/**
 * Thrown when an application is rejected during underwriting evaluation (p3-contract.md §3.1).
 * Handled specifically to return HTTP 422 with { details: [{ "reason": reason }] }.
 */
public class ApplicationRejectedException extends RuntimeException {

    private final String reason;

    public ApplicationRejectedException(String reason) {
        super("Application rejected: " + reason);
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }
}
