package com.team11.insurance.notifications;

/**
 * Lifecycle states of an outbox notification delivery item (Component C9 - SAD §4.1.7 & ADR-005).
 */
public enum NotificationStatus {
    /**
     * Persisted in the transactional outbox; pending initial dispatch or retry.
     */
    PENDING,

    /**
     * Currently claimed by an asynchronous worker thread for channel dispatch.
     */
    PROCESSING,

    /**
     * Successfully dispatched across all configured channels (or primary/fallback).
     */
    SENT,

    /**
     * Terminal delivery failure after exhausting max retries or encountering a non-retryable error.
     * Retained permanently in the dead-letter log for audit and operational diagnosis.
     */
    FAILED
}
